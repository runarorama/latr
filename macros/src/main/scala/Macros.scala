package lazified

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import scala.annotation.StaticAnnotation
import scala.annotation.compileTimeOnly

@compileTimeOnly("enable macro paradise to expand macro annotations")
object lazifyMacro {
  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    import Flag._

    def go(mods: Modifiers, tname: TermName, tpt: Tree, expr: Tree) = {
      val TermName(tn) = tname
      val haz = TermName(c.freshName(s"${tn}_haz$$"))
      val memo = TermName(s"${tn}_memo$$")
      q"""
        var $haz: Boolean = _
        var $memo: $tpt = _

        $mods def $tname: $tpt = {
          if (!$haz) {
            $haz = true
            $memo = $expr
          }
          $memo
        }
      """
    }

    val result = {
      annottees.map(_.tree).toList match {
        case q"$mods val $tname: $tpt = $expr" :: Nil => go(mods, tname, tpt, expr)
        case q"$mods def $tname: $tpt = $expr" :: Nil => go(mods, tname, tpt, expr)
        case _ => c.abort(c.enclosingPosition,
          "lazify macro must annotate a val or def, with explicit type annotation. For example `@lazify val x: Int = 0`")
      }
    }
    c.Expr[Any](result)
  }
}

class lazify extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro lazifyMacro.impl
}

@compileTimeOnly("enable macro paradise to expand macro annotations")
object lazifyPessimisticMacro {
  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    import Flag._

    def go(mods: Modifiers, tname: TermName, tpt: Tree, expr: Tree) = {
      val TermName(tn) = tname
      val haz = TermName(c.freshName(s"${tn}_haz$$"))
      val memo = TermName(c.freshName(s"${tn}_memo$$"))
      val lazyCompute = TermName(c.freshName(s"${tn}_lazyCompute$$"))
      val monitor = TermName(c.freshName(s"${tn}_monitor$$"))

      q"""
        val $monitor = new Object {}
        @volatile var $haz: Int = 0
        var $memo: $tpt = _
        private def $lazyCompute(): $tpt = {
          $monitor.synchronized {
            if ($haz == 0) {
              $haz = 1
            } else {
              while ($haz == 1) {
                $monitor.wait()
              }
              return $memo
            }
          }
          val result = $expr
          $monitor.synchronized {
            $memo = result
            $haz = 3
            $monitor.notifyAll()
          }
          $memo
        }
        def $tname: $tpt = if ($haz == 3) $memo else $lazyCompute()
      """
    }

    val result = {
      annottees.map(_.tree).toList match {
        case q"$mods val $tname: $tpt = $expr" :: Nil => go(mods, tname, tpt, expr)
        case q"$mods def $tname: $tpt = $expr" :: Nil => go(mods, tname, tpt, expr)
        case _ => c.abort(c.enclosingPosition,
          "lazifyPessimistic macro must annotate a val or def, with explicit type annotation. For example `@lazifyOptimistic val x: Int = 0`")
      }
    }
    c.Expr[Any](result)
  }
}

class lazifyPessimistic extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro lazifyPessimisticMacro.impl
}

@compileTimeOnly("enable macro paradise to expand macro annotations")
object lazifyOptimisticMacro {
  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    import Flag._

    def go(mods: Modifiers, tname: TermName, tpt: Tree, expr: Tree) = {
      val TermName(tn) = tname
      val haz = TermName(c.freshName(s"${tn}_haz$$"))
      val memo = TermName(c.freshName(s"${tn}_memo$$"))

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
      annottees.map(_.tree).toList match {
        case q"$mods val $tname: $tpt = $expr" :: Nil => go(mods, tname, tpt, expr)
        case q"$mods def $tname: $tpt = $expr" :: Nil => go(mods, tname, tpt, expr)
        case _ => c.abort(c.enclosingPosition,
          "lazifyOptimistic macro must annotate a val or def, with explicit type annotation. For example `@lazifyOptimistic val x: Int = 0`")
      }
    }
    c.Expr[Any](result)
  }
}

class lazifyOptimistic extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro lazifyOptimisticMacro.impl
}
