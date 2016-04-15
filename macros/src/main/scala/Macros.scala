import scala.language.experimental.macros
import scala.reflect.macros.Context
import scala.annotation.StaticAnnotation
import scala.annotation.compileTimeOnly

@compileTimeOnly("enable macro paradise to expand macro annotations")
object lazifyMacro {
  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    import Flag._

    def go(mods: Modifiers, tname: TermName, tpt: Tree, expr: Tree) = {
      val TermName(tn) = tname
      val haz = TermName(s"$tn$$haz$$")
      val memo = TermName(s"$tn$$memo$$")
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
        //case q"$mods val $tname = $expr" => go(mods, tname, q"", expr)
      }
    }
    c.Expr[Any](result)
  }
}

class lazify extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro lazifyMacro.impl
}

