package edu.sokolov.lab1.ssa

import org.jetbrains.kotlin.psi.KtExpression

interface Expr : Node {
    data class Stub(val expr: KtExpression): Expr {
        override fun toString(): String = expr.text
    }
}

data class BinaryExpr(val left: Expr, val op: String, val right: Expr) : Expr