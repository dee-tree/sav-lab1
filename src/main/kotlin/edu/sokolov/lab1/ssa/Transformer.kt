package edu.sokolov.lab1.ssa

import kotlin.reflect.KFunction
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.ElementType
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.fir.builder.toBinaryName
import org.jetbrains.kotlin.fir.builder.toFirOperation
import org.jetbrains.kotlin.fir.builder.toFirOperationOrNull
import org.jetbrains.kotlin.fir.resolve.dfa.isEq
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class Transformer(val ctx: Context = Context()) {
    fun transform(decl: KtDeclaration): Statement {
        return if (decl is KtFunction) {
            decl.bodyBlockExpression?.let { transformBlock(it, ::transformStatement) }
            TODO()
        } else TODO()
    }

    private fun transformStatement(statement: KtExpression): Statement {
        return when (statement) {
            is KtProperty -> transformProperty(statement, ctx, ::transformExpr)
            is KtReturnExpression ->
            else -> TODO("${statement.text} | ${statement::class.simpleName}")
//        is KtBinaryExpression -> transformBinaryExpr(this)
        }
    }

    private fun transformExpr(expr: KtExpression): Expr {
        return when (expr) {
            is KtBinaryExpression -> transformBinaryExpr(expr, ::transformExpr)
            is KtStringTemplateExpression -> Expr.Stub(expr)
            else -> TODO("${expr.text} | ${expr::class.simpleName}")
        }
    }



}

class TransformException(override val message: String = "Transformation error") : Exception(message) {

}

private fun transformProperty(expr: KtProperty, ctx: Context, transformAssignment: (KtExpression) -> Expr): Assignment {
    return expr.initializer?.let {
        Assignment(ctx.forName(expr.name!!), transformAssignment(it))
    } ?: throw TransformException()
}

private fun transformBinaryExpr(expr: KtBinaryExpression, transformOperand: (KtExpression) -> Expr): Expr {
    return BinaryExpr(transformOperand(expr.left!!), expr.operationReference.text, transformOperand(expr.right!!))
}

private fun KtIfExpression.transform() {

}

fun foo(x: Int) {
    var y = if (x < 3) {
        x * 2
    } else { 2 }

    y += 2
    
}



//fun transformBinaryExpr(expr: KtBinaryExpression) {
//    when (expr.operationReference.operationSignTokenType?.value) {
//        "=" -> println("It's assignment! ${expr}")
//    }
//}

private fun transformBlock(expr: KtBlockExpression, transformStatement: (KtExpression) -> Statement): Statement {
    expr.statements.map(transformStatement)//.let { BasicBlock(it) // }
    return BasicBlock(emptyList(), BasicBlock.Exit.NoNext)
}