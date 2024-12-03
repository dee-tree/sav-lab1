package edu.sokolov.lab1.ssa

import edu.sokolov.lab1.stat.collectStats
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtWhileExpression
import org.jetbrains.kotlin.psi.stubs.ConstantValueKind

class Transformer(val ctx: Context = Context()) {
    // returns the last basic block
    fun transform(decl: KtDeclaration): BasicBlock {
        val head = BasicBlock(name = "entry")
        if (decl is KtFunction) {
            transformFunction(decl, head)
        } else if (decl is KtClass) { transformClassDecl(decl) }
        return head
    }

    private fun transformFunction(func: KtFunction, head: BasicBlock): BasicBlock {
        ctx.push()
        func.valueParameters.forEach {
            ctx.introduce(it.name!!)
        }

        return when {
            func.bodyExpression != null -> transformExpr(func.bodyExpression!!, head)
            func.bodyBlockExpression != null -> transformBlock(func.bodyBlockExpression!!, head)
            else -> throw IllegalStateException("Function must have a body")
        }.also { ctx.pop() }
    }

    private fun transformExpr(expr: KtExpression, bb: BasicBlock): BasicBlock {
        return when (expr) {
            is KtIfExpression -> transformIf(expr, bb)
            is KtForExpression -> transformForLoop(expr, bb)
            is KtWhileExpression -> transformWhileLoop(expr, bb)
            is KtCallExpression -> transformCallExpr(expr, bb)
            is KtBinaryExpression -> transformBinaryExpr(expr, bb)
            is KtPrefixExpression -> transformPrefixExpr(expr, bb)
            is KtPostfixExpression -> transformPostfixExpr(expr, bb)
            is KtConstantExpression -> transformConstExpr(expr, bb)
            is KtStringTemplateExpression -> transformStringTemplateExpr(expr, bb)
            is KtBlockExpression -> transformBlock(expr, bb)
            is KtProperty -> transformProperty(expr, bb)
            is KtReturnExpression -> transformReturnExpr(expr, bb)
            is KtNameReferenceExpression -> transformNameReferenceExpr(expr, bb)
            is KtDotQualifiedExpression -> transformDotQualifiedExpr(expr, bb)
            else -> TODO("${expr.text} | ${expr::class.simpleName}")
        }
    }

    // returns exit merged block
    private fun transformIf(expr: KtIfExpression, bb: BasicBlock): BasicBlock {
        val headDefinitions = ctx.definitions(bb)
        val merged = BasicBlock(name = "if_merged")
        val trueBranch = BasicBlock(name = "if_true")

        headDefinitions.forEach { def ->
            trueBranch += Assignment(def.definition.new(), PhiExpr(def))
        }

        transformExpr(expr.then!!, trueBranch).also { it.exit = BasicBlock.Exit.Unconditional(merged) }
//        trueBranch += Assignment(ctx.fresh("if_res"), trueBranch.children.last().lhs)
        val falseBranch = expr.`else`?.let { elseExpr ->
            val b = BasicBlock(name = "if_false")

            headDefinitions.forEach { def ->
                b += Assignment(def.definition.new(), PhiExpr(def))
            }

            transformExpr(elseExpr, b).also { it.exit = BasicBlock.Exit.Unconditional(merged) }
//            b += Assignment(ctx.fresh("if_res"), b.children.last().lhs)
            b
        } ?: merged

        val condBlock = BasicBlock(name = "if_cond")

        headDefinitions.forEach { def ->
            condBlock += Assignment(def.definition.new(), PhiExpr(def))
        }

        bb.exit = BasicBlock.Exit.Unconditional(condBlock)
        transformExpr(expr.condition!!, condBlock).also {
            it.exit = BasicBlock.Exit.Conditional(it.children.last().lhs, trueBranch, falseBranch)
        }

        ctx.mergedDefinitions(trueBranch, falseBranch).forEachIndexed { i, def ->
            val stamp = ctx.fresh(def.ofName)
            merged += Assignment(stamp, def)
//            if (i == 0) merged += Assignment(ctx.fresh("if_res"), stamp)
        }

        return merged
    }

    private fun transformReturnExpr(expr: KtReturnExpression, bb: BasicBlock): BasicBlock {
        val nextBB = transformExpr(expr.returnedExpression!!, bb)
        nextBB.exit = BasicBlock.Exit.Ret(nextBB.children.last().lhs)
        return nextBB
    }

    private fun transformNameReferenceExpr(expr: KtNameReferenceExpression, bb: BasicBlock): BasicBlock {
        val resolved = ctx.resolve(expr.text, bb)
        bb += Assignment(ctx.tmp(), resolved)
        return bb
    }

    private fun transformDotQualifiedExpr(expr: KtDotQualifiedExpression, bb: BasicBlock): BasicBlock {
        val resolvedBase = ctx.resolve(expr.receiverExpression.text, bb)

        var nextBB = bb
        if (expr.selectorExpression is KtCallExpression) {
            val call = expr.selectorExpression as KtCallExpression
            val originArgs = call.valueArguments
            val args = originArgs.map { arg ->
                nextBB = transformExpr(arg.children.last() as KtExpression, nextBB)
                val argStamp = nextBB.children.last().lhs
                if (arg.isNamed()) NamedArgument(arg.getArgumentName()!!.text, argStamp) else argStamp
            }
            nextBB += Assignment(ctx.tmp(), MemberCallExpr(base = resolvedBase, method = call.calleeExpression!!.text, args = args))
        } else {
            nextBB += Assignment(ctx.tmp(), ctx.resolve("${resolvedBase.definition.name}.${expr.selectorExpression!!.text}", bb))
        }

        return nextBB
    }

    private fun transformPostfixExpr(expr: KtPostfixExpression, bb: BasicBlock): BasicBlock {
        // a++ => res = a; a += 1; res
        val op = when (expr.operationToken) {
            KtTokens.PLUSPLUS -> KtTokens.PLUS.value
            KtTokens.MINUSMINUS -> KtTokens.MINUS.value
            else -> throw IllegalStateException("Unknown postfix op: ${expr.operationToken} in $expr")
        }

        val base = ctx.resolve(expr.baseExpression!!.text, bb)
        val res = Assignment(ctx.tmp(), base)
        val one = Assignment(ctx.tmp(), ConstExpr.IntLiteral(1)).also { bb += it }.lhs
        Assignment(base.definition.new(), BinaryExpr(base, op, one)).also { bb += it }
        bb += res

        return bb
    }

    private fun transformPrefixExpr(expr: KtPrefixExpression, bb: BasicBlock): BasicBlock {
        // ++a => a += 1; res = a; res
        val op = when (expr.operationToken) {
            KtTokens.PLUSPLUS -> KtTokens.PLUS.value
            KtTokens.MINUSMINUS -> KtTokens.MINUS.value
            else -> throw IllegalStateException("Unknown prefix op: ${expr.operationToken} in $expr")
        }

        val base = ctx.resolve(expr.baseExpression!!.text, bb)
        val one = Assignment(ctx.tmp(), ConstExpr.IntLiteral(1)).also { bb += it }.lhs
        Assignment(base.definition.new(), BinaryExpr(base, op, one)).also { bb += it }

        return bb
    }

    private fun transformAssignment(lhs: KtNameReferenceExpression, rhs: KtExpression, bb: BasicBlock): BasicBlock {
        val resolved = ctx.resolve(lhs.text, bb).definition.new()
        val nextBB = transformExpr(rhs, bb)
        nextBB += Assignment(resolved, nextBB.children.last().lhs)
        return nextBB
    }

    private fun transformBinaryExpr(expr: KtBinaryExpression, bb: BasicBlock): BasicBlock {
        if (expr.operationToken == KtTokens.EQ) return transformAssignment(expr.left as KtNameReferenceExpression, expr.right!!, bb)

        var nextBB = transformExpr(expr.left!!, bb)
        val lhs = nextBB.children.last().lhs

        nextBB = transformExpr(expr.right!!, nextBB)
        val rhs = nextBB.children.last().lhs

        val binex = BinaryExpr(lhs, expr.operationReference.text, rhs)
        val binas = Assignment(ctx.tmp(), binex)
        nextBB += binas
        return nextBB
    }

    private fun transformBlock(expr: KtBlockExpression, bb: BasicBlock): BasicBlock {
        ctx.push()
        var nextBB = bb
        expr.statements.forEach { statement ->
            nextBB = transformExpr(statement, nextBB)
        }
        ctx.pop()
        return nextBB
    }

    private fun transformProperty(expr: KtProperty, bb: BasicBlock): BasicBlock {
        val id = ctx.introduce(expr.name!!)
        val nextBB= expr.initializer?.let { initializer ->
            transformExpr(initializer, bb).also {
                it += Assignment.introduce(id, it.children.last().lhs)
            }
        } ?: run {
            bb += Assignment.introduce(id)
            bb
        }
        return nextBB
    }

    private fun transformForLoop(expr: KtForExpression, bb: BasicBlock): BasicBlock {
        ctx.push()

        var nextBB = transformExpr(expr.loopRange!!, bb)
        val loopRange = nextBB.children.last().let { asg ->
            if (asg.rhs is Definition.Stamp) asg.rhs
            else {
                val stamp = ctx.introduce("for_lab\$${asg.lhs.definition.name}${asg.lhs.id}")
                nextBB += Assignment(stamp, asg.lhs)
                stamp
            }
        }

        val loopCondBlock = BasicBlock(name = "for_cond")
        ctx.definitions(nextBB).forEach { def -> loopCondBlock += Assignment(def.definition.new(), PhiExpr(def)) }

        nextBB.exit = BasicBlock.Exit.Unconditional(loopCondBlock)
        loopCondBlock.addPredecessor(nextBB)

        val loopParam = ctx.introduce(expr.loopParameter!!.text)

        loopCondBlock += Assignment(loopParam, MemberCallExpr(ctx.resolve(loopRange.definition.name, loopCondBlock), "next"))

        val loopBlock = BasicBlock(name = "for_body")
        ctx.definitions(loopCondBlock).forEach { def -> loopBlock += Assignment(def.definition.new(), PhiExpr(def)) }
        loopBlock.addPredecessor(loopCondBlock)

        nextBB = transformExpr(expr.body!!, loopBlock)

        loopCondBlock.addPredecessor(nextBB)

        val mergedBlock = BasicBlock(name = "for_merged")
        ctx.definitions(loopCondBlock).forEach { def -> if (def != loopParam) mergedBlock += Assignment(def.definition.new(), PhiExpr(def)) }
        mergedBlock.addPredecessor(loopCondBlock)

        loopBlock.exit = BasicBlock.Exit.Unconditional(loopCondBlock)

        loopCondBlock.exit = BasicBlock.Exit.Conditional(BinaryExpr(loopParam, "==", ConstExpr.NullLiteral), mergedBlock, loopBlock)

        ctx.pop()
        return bb
    }

    companion object {
        private var loopIdx = 0
    }

    fun transformWhileLoop(expr: KtWhileExpression, bb: BasicBlock): BasicBlock {
        val condBeforeStart = bb.children.size
        val condBeforeBlock = transformExpr(expr.condition!!, bb)

        val _loopVarsBefore = condBeforeBlock.children.drop(condBeforeStart).toSet()
        val loopVarsBefore = _loopVarsBefore.mapIndexed { i, v ->
            val def = if (with(ctx) { v.lhs.isTmp }) { if (with(ctx) { (v.rhs as? Definition.Stamp)?.isTmp == false}) (v.rhs as Definition.Stamp).definition.new() else ctx.fresh("ploop${loopIdx}-$i").also { condBeforeBlock += Assignment(it, v.rhs) } } else v.lhs.definition.new()
            def
        }
        loopIdx++

        val loopVars = loopVarsBefore.map { it.definition }.toSet()

        val loopStart = BasicBlock(name = "while_start")
        loopStart.addPredecessor(condBeforeBlock)
        condBeforeBlock.exit = BasicBlock.Exit.Unconditional(loopStart)

        val loopVarsAtStart = loopVars.map { it.new() }

        val startPhis = loopVarsAtStart.map { v ->
            val asg = Assignment(v, PhiExpr(ctx.resolve(v.definition.name, condBeforeBlock)))
            loopStart += asg
            asg
        }

        val cond = loopStart.children.last().lhs

        val loopBody = BasicBlock(name = "while_body")
        loopBody.addPredecessor(loopStart)
        ctx.definitions(loopStart).forEach { def -> loopBody += Assignment(def.definition.new(), PhiExpr(def)) }

        val loopBodyNext = transformExpr(expr.body!!, loopBody)
        loopBodyNext.exit = BasicBlock.Exit.Unconditional(loopStart)
        loopStart.addPredecessor(loopBodyNext)

        val bodyEndVars = loopVars.map { def -> ctx.resolve(def.name, loopBodyNext) }
        startPhis.forEach { phi ->
            val endvar = bodyEndVars.find { it.definition == phi.lhs.definition } ?: return@forEach
            (phi.rhs as PhiExpr) += endvar
        }

        val loopExit = BasicBlock(name = "while_exit")

        loopVarsBefore.zip(loopVarsAtStart).forEach { (a, b) ->
            loopExit += Assignment(a.definition.new(), PhiExpr(a, b))
        }

        loopStart.exit = BasicBlock.Exit.Conditional(cond, loopBody, loopExit)
        loopExit.addPredecessor(loopStart)

       return loopExit
    }

    private fun transformCallExpr(expr: KtCallExpression, bb: BasicBlock): BasicBlock {
        var nextBB = bb

        val args = expr.valueArguments.map { arg ->
            nextBB = transformExpr(arg.children.last() as KtExpression, nextBB)
            val argStamp = nextBB.children.last().lhs
            if (arg.isNamed()) NamedArgument(arg.getArgumentName()!!.text, argStamp) else argStamp
        }

        nextBB = transformExpr(expr.calleeExpression!!, nextBB)
        val callee = nextBB.children.last().lhs
        nextBB += Assignment(ctx.tmp(), CallExpr(callee, args))

        return nextBB
    }

    private fun transformClassDecl(decl: KtClass) {
        println("in class decl: ${decl.fqName}")
        ctx.stats += decl.collectStats()
    }

    private fun transformLiteralStringTemplateEntry(expr: KtLiteralStringTemplateEntry, bb: BasicBlock): Definition.Stamp {
        val res = ctx.tmp()
        bb += Assignment(res, ConstExpr.StringLiteral(expr.text!!))
        return res
    }

    private fun transformSimpleNameTemplateEntry(expr: KtSimpleNameStringTemplateEntry, bb: BasicBlock): BasicBlock {
        val nextBB = transformExpr(expr.expression!!, bb)
        nextBB += Assignment(ctx.tmp(), MemberCallExpr(nextBB.children.last().lhs, "toString"))
        return nextBB
    }

    private fun transformBlockTemplateEntry(expr: KtBlockStringTemplateEntry, bb: BasicBlock): BasicBlock {
        val nextBB = transformExpr(expr.expression!!, bb)
        nextBB += Assignment(ctx.tmp(), MemberCallExpr(nextBB.children.last().lhs, "toString"))
        return nextBB
    }

    private fun transformStringTemplateExpr(expr: KtStringTemplateExpression, bb: BasicBlock): BasicBlock {
        var nextBB = bb

        val parts = expr.children.map { part ->
            when (part) {
                is KtLiteralStringTemplateEntry -> transformLiteralStringTemplateEntry(part, bb)
                is KtSimpleNameStringTemplateEntry -> {
                    nextBB = transformSimpleNameTemplateEntry(part, bb)
                    nextBB.children.last().lhs
                }
                is KtBlockStringTemplateEntry -> {
                    nextBB = transformBlockTemplateEntry(part, bb)
                    nextBB.children.last().lhs
                }
                else -> throw IllegalArgumentException("Unexpected expr in string interpolation: $part of class ${part::class}")
            }
        }

        if (parts.size > 1) {
            var op1 = parts[0]

            for (op2 in parts.subList(1, parts.size)) {
                val resStamp = ctx.tmp()
                nextBB += Assignment(resStamp, BinaryExpr(op1, KtTokens.PLUS.value, op2))
                op1 = resStamp
            }
        }

        return nextBB
    }

    private fun transformConstExpr(expr: KtConstantExpression, bb: BasicBlock): BasicBlock {
        val id = ctx.tmp()
        val const = when {
            expr.toString() == ConstantValueKind.BOOLEAN_CONSTANT.toString() -> ConstExpr.BoolLiteral(expr.text.toBooleanStrict())
            expr.toString() == ConstantValueKind.INTEGER_CONSTANT.toString() && expr.text.contains('u', true) && expr.text.contains('l', true) -> ConstExpr.ULongLiteral(expr.text.toULong())
            expr.toString() == ConstantValueKind.INTEGER_CONSTANT.toString() && expr.text.contains('l', true) -> ConstExpr.LongLiteral(expr.text.toLong())
            expr.toString() == ConstantValueKind.INTEGER_CONSTANT.toString() && expr.text.contains('u', true) -> ConstExpr.UIntLiteral(expr.text.toUInt())
            expr.toString() == ConstantValueKind.INTEGER_CONSTANT.toString() -> ConstExpr.IntLiteral(expr.text.toInt())
            expr.toString() == ConstantValueKind.FLOAT_CONSTANT.toString() && expr.text.contains('f', true) -> ConstExpr.FloatLiteral(expr.text.toFloat())
            expr.toString() == ConstantValueKind.FLOAT_CONSTANT.toString() -> ConstExpr.DoubleLiteral(expr.text.toDouble())
            expr.toString() == ConstantValueKind.NULL.toString() -> ConstExpr.NullLiteral
            expr.toString() == ConstantValueKind.CHARACTER_CONSTANT.toString() -> ConstExpr.CharLiteral(expr.text[0])
            else -> throw IllegalArgumentException("Unexpected const $expr with value ${expr.text}")
        }
        bb += Assignment(id, const)
        return bb
    }

}

class TransformException(override val message: String = "Transformation error") : Exception(message) {

}
