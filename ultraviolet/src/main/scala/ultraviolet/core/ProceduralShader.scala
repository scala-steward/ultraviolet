package ultraviolet.core

import scala.annotation.tailrec
import scala.deriving.Mirror
import scala.quoted.*

final case class ProceduralShader(defs: List[ShaderAST], main: ShaderAST)

object ProceduralShader:
  given ToExpr[ProceduralShader] with {
    def apply(x: ProceduralShader)(using Quotes): Expr[ProceduralShader] =
      '{ ProceduralShader(${ Expr(x.defs) }, ${ Expr(x.main) }) }
  }

  extension (p: ProceduralShader)
    inline def toGLSL[In](using Mirror.ProductOf[In]): String =
      // println(EnvReader.readUBO[In]) // TODO: Use this.
      p.render

    inline def render: String =
      import ShaderAST.*
      def envName(ast: ShaderAST): Option[String] =
        ast
          .find {
            case ShaderBlock(_, _, _) => true
            case _                    => false
          }
          .flatMap {
            case ShaderBlock(name, _, _) => name
            case _                       => None
          }

      val res = (p.defs ++ List(p.main)).map(_.render).mkString("\n").trim

      envName(p.main) match
        case None       => res
        case Some(name) => res.replace(name + ".", "").replace(name, "")

    def exists(q: ShaderAST => Boolean): Boolean =
      p.main.exists(q) || p.defs.exists(_.exists(q))
