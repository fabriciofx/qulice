/*
 * SPDX-FileCopyrightText: Copyright (c) 2011-2025 Yegor Bugayenko
 * SPDX-License-Identifier: MIT
 */

package com.qulice.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.util.LinkedList;
import java.util.List;

/**
 * Checks that constructor, declared as private class is used more than once.
 *
 * @since 0.3
 */
public final class ProhibitUnusedPrivateConstructorCheck extends AbstractCheck {

    @Override
    public int[] getDefaultTokens() {
        return new int[] {TokenTypes.CLASS_DEF};
    }

    @Override
    public int[] getAcceptableTokens() {
        return this.getDefaultTokens();
    }

    @Override
    public int[] getRequiredTokens() {
        return this.getDefaultTokens();
    }

    @Override
    public void visitToken(final DetailAST ast) {
        final DetailAST objblock = ast.findFirstToken(TokenTypes.OBJBLOCK);
        if (objblock != null) {
            this.checkConstructors(objblock);
        }
    }

    /**
     * Collects all private constructors in a given object block.
     *
     * @param objblock Node which contains constructors
     * @return List of DetailAST nodes representing the private constructors
     */
    private static List<DetailAST> collectPrivateConstructors(final DetailAST objblock) {
        final List<DetailAST> prvctors = new LinkedList<>();
        final DetailAST firstchld = objblock.getFirstChild();
        for (DetailAST child = firstchld; child != null; child = child.getNextSibling()) {
            if (child.getType() == TokenTypes.CTOR_DEF && isPrivate(child)) {
                prvctors.add(child);
            }
        }
        return prvctors;
    }

    /**
     * Checks if a private constructor is used in the object block.
     *
     * @param privatector Node representing the private constructor
     * @param objblock Node which contains constructors
     * @return True if the private constructor is used, False otherwise
     */
    private static boolean isPrivateConstructorUsed(
        final DetailAST privatector, final DetailAST objblock) {
        return
            isPrivateCtorUsedInOtherCtors(privatector, objblock)
            ||
            isPrivateCtorUsedInMethods(privatector, objblock);
    }

    /**
     * Checks if a private constructor is used in other constructors.
     *
     * @param privatector Node representing the private constructor
     * @param objblock Node containing constructors
     * @return True if the private constructor is used, False otherwise
     */
    private static boolean isPrivateCtorUsedInOtherCtors(
        final DetailAST privatector, final DetailAST objblock) {
        final List<DetailAST> allctors = collectAllConstructors(objblock);
        return allctors.stream()
            .anyMatch(
                otherCtor -> otherCtor != privatector
                &&
                isCallingConstructor(otherCtor, privatector)
            );
    }

    /**
     * Checks if a private constructor is used in methods of the object block.
     *
     * @param privatector Node representing the private constructor
     * @param objblock Node containing methods
     * @return True if the private constructor is used, False otherwise
     */
    private static boolean isPrivateCtorUsedInMethods(
        final DetailAST privatector, final DetailAST objblock) {
        boolean result = false;
        final DetailAST firstchld = objblock.getFirstChild();
        for (DetailAST child = firstchld; child != null; child = child.getNextSibling()) {
            if (child.getType() == TokenTypes.METHOD_DEF
                &&
                isCallingConstructor(child, privatector)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * Collects all constructors in a given object block.
     *
     * @param objblock Node which contains constructors
     * @return List of DetailAST nodes representing all the constructors
     */
    private static List<DetailAST> collectAllConstructors(final DetailAST objblock) {
        final List<DetailAST> allctors = new LinkedList<>();
        final DetailAST firstchld = objblock.getFirstChild();
        for (DetailAST child = firstchld; child != null; child = child.getNextSibling()) {
            if (child.getType() == TokenTypes.CTOR_DEF) {
                allctors.add(child);
            }
        }
        return allctors;
    }

    /**
     * Returns true if specified node has modifiers of type
     * <code>PRIVATE</code>.
     *
     * @param node Node to check.
     * @return True if specified node contains modifiers of type
     *  <code>PRIVATE</code>, else returns <code>false</code>.
     */
    private static boolean isPrivate(final DetailAST node) {
        final DetailAST modifiers = node.findFirstToken(TokenTypes.MODIFIERS);
        return modifiers.getChildCount(TokenTypes.LITERAL_PRIVATE) > 0;
    }

    private static boolean isCallingConstructor(
        final DetailAST methodorctor, final DetailAST targetctor) {
        boolean result = false;
        final DetailAST body = methodorctor.findFirstToken(TokenTypes.SLIST);
        if (body != null) {
            DetailAST stmt = body.getFirstChild();
            while (stmt != null && !result) {
                result = isMatchingConstructorCall(stmt, targetctor);
                stmt = stmt.getNextSibling();
            }
        }
        return result;
    }

    private static boolean isMatchingConstructorCall(
        final DetailAST stmt, final DetailAST targetctor) {
        return
            stmt.getType() == TokenTypes.CTOR_CALL
            &&
            matchesConstructorSignature(stmt, targetctor);
    }

    private static boolean matchesConstructorSignature(
        final DetailAST callexpr, final DetailAST ctor) {
        final DetailAST callparams = callexpr.findFirstToken(TokenTypes.ELIST);
        final DetailAST ctorparams = ctor.findFirstToken(TokenTypes.PARAMETERS);
        return parametersCountMatch(callparams, ctorparams);
    }

    private static boolean parametersCountMatch(
        final DetailAST callparams, final DetailAST ctorparams) {
        final int ncallparams = callparams.getChildCount(TokenTypes.EXPR);
        final int nctorparams = ctorparams.getChildCount(TokenTypes.PARAMETER_DEF);
        return ncallparams == nctorparams;
    }

    /**
     * Checks if private constructors are used.
     * Logs a message if a private constructor is not used.
     *
     * @param objblock Node which contains constructors
     */
    private void checkConstructors(final DetailAST objblock) {
        final List<DetailAST> prvctors = collectPrivateConstructors(objblock);
        for (final DetailAST ctor : prvctors) {
            if (!isPrivateConstructorUsed(ctor, objblock)) {
                this.log(ctor.getLineNo(), "Unused private constructor.");
            }
        }
    }

}
