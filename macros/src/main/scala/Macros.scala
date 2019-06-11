package latr

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import scala.annotation.StaticAnnotation
import scala.annotation.compileTimeOnly

@compileTimeOnly("enable macro paradise to expand macro annotations")
class latrMacro(val c: Context) {
  import c.universe._
  import Flag._

  def lazifyImpl(annottees: Tree*): Tree = {
    def go(mods: Modifiers, tname: TermName, tpt: Tree, expr: Tree) = {
      val TermName(tn) = tname
      val haz = TermName(c.freshName(s"${tn}$$haz"))
      val memo = TermName(s"${tn}$$memo")

      q"""
        var $haz: Boolean = _
        var $memo: $tpt = _

        $mods def $tname: $tpt = {
          if (!$haz) {
            $memo = $expr
            $haz = true
          }
          $memo
        }
      """
    }

    val result = {
      annottees.toList match {
        case q"$mods val $tname: $tpt = $expr" :: Nil => go(mods, tname, tpt, expr)
        case q"$mods def $tname: $tpt = $expr" :: Nil => go(mods, tname, tpt, expr)
        case _ => c.abort(c.enclosingPosition,
          "lazify macro must annotate a val or def, with explicit type annotation. For example `@lazify val x: Int = 0`")
      }
    }
    result
  }

  def pessimisticImpl(annottees: Tree*): Tree = {
    def go(mods: Modifiers, tname: TermName, tpt: Tree, expr: Tree) = {
      val TermName(tn) = tname
      val haz = TermName(c.freshName(s"${tn}$$haz"))
      val memo = TermName(c.freshName(s"${tn}$$memo"))
      val lazyCompute = TermName(c.freshName(s"${tn}$$latrCompute"))

      q"""
        @volatile var $haz: Byte = 0
        var $memo: $tpt = _
        private def $lazyCompute(): $tpt = {
          this.synchronized {
            if ($haz == 0) {
              $haz = 1
            } else {
              while ($haz == 1) {
                this.wait()
              }
              return $memo
            }
          }
          val result = $expr
          this.synchronized {
            $memo = result
            $haz = 3
            this.notifyAll()
          }
          $memo
        }
        def $tname: $tpt = if ($haz == 3) $memo else $lazyCompute()
      """
    }

    val result = {
      annottees.toList match {
        case q"$mods val $tname: $tpt = $expr" :: Nil => go(mods, tname, tpt, expr)
        case q"$mods def $tname: $tpt = $expr" :: Nil => go(mods, tname, tpt, expr)
        case _ => c.abort(c.enclosingPosition,
          "lazifyPessimistic macro must annotate a val or def, with explicit type annotation. For example `@lazifyPessimistic val x: Int = 0`")
      }
    }
    result
  }

  def optimisticImpl(annottees: Tree*): Tree = {
    def go(mods: Modifiers, tname: TermName, tpt: Tree, expr: Tree) = {
      val TermName(tn) = tname
      val haz = TermName(c.freshName(s"${tn}$$haz"))
      val memo = TermName(c.freshName(s"${tn}$$memo"))

      q"""
        import annotation.{tailrec, switch}
        val $haz = new java.util.concurrent.atomic.AtomicInteger
        var $memo: $tpt = _

        @tailrec final def $tname: $tpt = ($haz.get: @switch) match {
          case 0 =>
            if ($haz.compareAndSet(0, 1)) {
              val result = $expr
              $memo = result
              if ($haz.getAndSet(3) != 1) $haz.synchronized { $haz.notify() }
              result
            } else $tname
          case 1 =>
            $haz.compareAndSet(1, 2)
            $haz.synchronized {
              while ($haz.get != 3) $haz.wait()
              $haz.notify()
            }
            $memo
          case 2 =>
            $haz.synchronized {
              while ($haz.get != 3) $haz.wait()
              $haz.notify()
            }
            $memo
          case 3 => $memo
        }
      """
    }

    val result = {
      annottees.toList match {
        case q"$mods val $tname: $tpt = $expr" :: Nil => go(mods, tname, tpt, expr)
        case q"$mods def $tname: $tpt = $expr" :: Nil => go(mods, tname, tpt, expr)
        case _ => c.abort(c.enclosingPosition,
          "lazifyOptimistic macro must annotate a val or def, with explicit type annotation. For example `@lazifyOptimistic val x: Int = 0`")
      }
    }
    result
  }
}

class lazify extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro latrMacro.lazifyImpl
}

class lazifyPessimistic extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro latrMacro.pessimisticImpl
}

class lazifyOptimistic extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro latrMacro.optimisticImpl
}
