package edu.sokolov.lab1.stat

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtVisitorVoid
import kotlin.math.max

fun KtClass.collectStats(): Stats {
    val noc = this.superTypeListEntries.size

    val otherClasses = hashSetOf<String>()
    val methods = hashSetOf<String>()
    val otherMethods = hashSetOf<String>()
    var totalMethodsArgs = 0
    val properties = hashSetOf<String>()

    // visit properties
    KtPsiUtil.visitChildren(this, object : KtVisitorVoid() {
        override fun visitClassBody(classBody: KtClassBody) {
            classBody.properties.forEach { property ->
                properties += property.name!!
            }
        }
    }, null)

    KtPsiUtil.visitChildren(this, object : KtTreeVisitorVoid() {
        override fun visitTypeReference(typeReference: KtTypeReference) {
            otherClasses += typeReference.getTypeText()
            super.visitTypeReference(typeReference)
        }

        override fun visitNamedFunction(function: KtNamedFunction) {
            methods += function.name!!
            totalMethodsArgs += function.valueParameters.size
            super.visitNamedFunction(function)
        }
    }, null)

    KtPsiUtil.visitChildren(this, object : KtTreeVisitorVoid() {
        override fun visitCallExpression(expression: KtCallExpression) {
            val name = expression.calleeExpression!!.text
            if (name !in methods) otherMethods += name
            super.visitCallExpression(expression)
        }
    }, null)

    val boundMethods = hashSetOf<Pair<String, String>>()
    val methodsProps = hashMapOf<String, HashSet<String>>().apply { methods.forEach { put(it, hashSetOf()) } }

    KtPsiUtil.visitChildren(this, object : KtTreeVisitorVoid() {
        override fun visitNamedFunction(function: KtNamedFunction) {
            KtPsiUtil.visitChildren(function, object : KtTreeVisitorVoid() {
                override fun visitReferenceExpression(expression: KtReferenceExpression) {
                    if (expression.text!! in properties) methodsProps[function.name]!! += expression.text
                    super.visitReferenceExpression(expression)
                }
            }, null)

        }
    }, null)

    for (prop in properties) {
        val bs = methodsProps.filter { (k, v) -> prop in v }.map { it.key }

        for (a in bs) {
            for (b in bs) {
                if (a == b) continue
                boundMethods += a to b
                boundMethods += b to a
            }
        }
    }

    val totalMethodsPairs = methods.size * (methods.size - 1) / 2
    val unboundMethods = totalMethodsPairs - boundMethods.size / 2

    return Stats(
        cls = this.fqName!!.toString(),
        noc = noc,
        cbo = otherClasses.size,
        rfc = methods.size + otherMethods.size,
        anam = totalMethodsArgs.toDouble() / methods.size,
        wcm2 = totalMethodsArgs,
        lcom = max(0, unboundMethods - boundMethods.size / 2)
    )
}