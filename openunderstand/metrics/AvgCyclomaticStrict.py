import os
from antlr4 import *
from setuptools import glob

from gen.javaLabeled.JavaLexer import JavaLexer
from gen.javaLabeled.JavaParserLabeled import JavaParserLabeled
from gen.javaLabeled.JavaParserLabeledListener import JavaParserLabeledListener

import argparse

class CyclomaticStrictListener(JavaParserLabeledListener):
    def __init__(self):
        self.count=0
        self.methods=0
        self.avg=0
        self.dict={}
        self.name=""

    @property
    def get_dict(self):
        return self.dict

    def enterClassDeclaration(self, ctx:JavaParserLabeled.ClassDeclarationContext):
        try:
            self.name = ctx.IDENTIFIER().getText()
            self.dict[self.name] = 0
            self.count=1
            self.methods=0
            self.avg = 0
        except:
            pass

    def enterCatchClause(self, ctx: JavaParserLabeled.CatchClauseContext):
        try:
            self.cnt(ctx,0)
        except:
            pass


    def enterMethodDeclaration(self, ctx:JavaParserLabeled.MethodDeclarationContext):
        try:
            self.methods = self.methods + 1
        except:
            pass

    def enterSwitchBlockStatementGroup(self, ctx:JavaParserLabeled.SwitchBlockStatementGroupContext):
        try:
            self.count=self.count+1
            if ctx.children[0].children[0].getText()=="default":
                self.count-=1
        except:
            pass
    #?
    def enterExpression20(self, ctx:JavaParserLabeled.Expression20Context):
        try:
            self.cnt(ctx,0)
        except:
            pass

   #and
    def enterExpression18(self, ctx: JavaParserLabeled.Expression18Context):
        try:
            self.cnt(ctx, 0)
        except:
            pass

    #or
    def enterExpression19(self, ctx: JavaParserLabeled.Expression18Context):
        try:
            self.cnt(ctx, 0)
        except:
            pass

    #if
    def enterStatement2(self, ctx: JavaParserLabeled.Statement3Context):
        try:
            if len(ctx.children) ==3:
                self.cnt(ctx,0)
            if len(ctx.children)==5:
                self.cnt(ctx, 1)
        except:
            pass

    #while
    def enterStatement4(self, ctx: JavaParserLabeled.Statement3Context):
        try:
            self.cnt(ctx,0)
        except:
            pass

    # for
    def enterStatement3(self, ctx: JavaParserLabeled.Statement3Context):
        try:
            self.cnt(ctx,0)
        except:
            pass

    #do-While
    def enterStatement5(self, ctx: JavaParserLabeled.Statement0Context):
        try:
            self.cnt(ctx, 2)
        except:
            pass

    def cnt(self,ctx,num):
        if ctx.children[0].getText() == "for":
            self.count=self.count+1

        if ctx.children[0].getText() == "while":
            self.count=self.count+1

        if ctx.children[0].getText() == "if":
            self.count = self.count + 1

        if ctx.children[0].getText() == "catch":
            self.count = self.count + 1

        if num==0 and ctx.children[0].getText() == "else":
            self.count = self.count + 1

        if  ctx.children[1].getText() == "?":
            self.count = self.count + 1

        if ctx.children[1].getText() == "&&":
            self.count = self.count + 1

        if ctx.children[1].getText() == "||":
            self.count = self.count + 1
                
        if num == 2 and ctx.children[0].getText() == "do" and ctx.children[2].getText() == "while":
            self.count = self.count + 1


    def exitClassDeclaration(self, ctx:JavaParserLabeled.ClassDeclarationContext):
        try:
            self.avg=self.count/self.methods
            self.dict[self.name] = self.avg
        except:
            pass



