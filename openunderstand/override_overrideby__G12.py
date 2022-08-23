import os
from antlr4 import *
from gen.javaLabeled.JavaLexer import JavaLexer
from gen.javaLabeled.JavaParserLabeled import JavaParserLabeled
from gen.javaLabeled.JavaParserLabeledListener import JavaParserLabeledListener
from analysis_passes import class_properties

DB_PATH = "../../database/calculator_app.oudb"
PROJECT_PATH = "../../benchmarks_projects/calculator_app"
PROJECT_NAME = "Calculator App"


#class decleration:
class ClassEntityListener(JavaParserLabeledListener):
    def __init__(self):
        self.class_body = None

    def enterClassDeclaration(self, ctx: JavaParserLabeled.ClassDeclarationContext):
        print("inside class declaration", ctx.getText())
        self.class_body = ctx.getText()



class overridelistener(JavaParserLabeledListener):
    def __init__(self):
        self.classes = {}
        self.extendedtoentity = []
        self.extendedlist = []
        self.dic = {}
        self.parent = []
        self.extend_word = False
        self.isclass = False
        self.FileName = None
        self.isoverride = False
        self.extendedname = ""
        self.class_Name = ""
        self.line = ''
        self.column = ''
        self.context = ''
        self.packageName = ""
        self.Methodkind = ""
        self.Imports = {}
        self.referencekind = ''
        self.getlongname = ''
        self.MethodLongname = ''

        self.modifiers = []
        self.properties = {}




#parent and long name by parentclasses
    @property
    def get_extendeds(self):
        return self.extendedlist
    @property
    def get_classes(self):
        return self.classes

    @property
    def get_db(self):
        return self.extendedtoentity

    def set_file(self, file):
  
        self.FileName = file

    def set_dictionary(self , dictionary):
        self.classes = dictionary


    def set_list(self , extendedlistx):
        self.extendedlist = extendedlistx


    def extract_original_text(self, ctx):
        token_source = ctx.start.getTokenSource()
        input_stream = token_source.inputStream
        start , stop = ctx.start.start , ctx.stop.stop
        return input_stream.getText(start , stop)


    def enterImportDeclaration(self, ctx:JavaParserLabeled.ImportDeclarationContext):
        imported_class_longname = ctx.qualifiedName().getText()
        imported_class_name = imported_class_longname.split('.')[-1]
        self.Imports[imported_class_name] = imported_class_longname




    def enterClassDeclaration(self, ctx: JavaParserLabeled.ClassDeclarationContext):

        self.class_Name = ctx.IDENTIFIER().getText()
        keyword = self.packageName + '.' + self.class_Name
        self.classes[keyword] = []
        if ctx.EXTENDS() != None:
            self.extend_word = True
            self.classes[keyword] = []
            self.isclass = True


    def enterMethodDeclaration(self, ctx: JavaParserLabeled.MethodDeclarationContext):
        mymethod = ctx.getText().split('(')
        keyword = self.packageName + '.' +self.class_Name

        if(True):
            scope_parents = class_properties.ClassPropertiesListener.findParents(ctx)

            
            if len(scope_parents) == 1:
                scope_longname = scope_parents[0]
            else:
                scope_longname = ".".join(scope_parents)

            [line, col] = str(ctx.start).split(",")[3].split(":")
            ##############
           
            string = ''
            for m in self.modifiers:
                string = m + ''

            self.dic = { "MethodIs":mymethod[0] , "scope_kind": "Method", "scope_name": ctx.IDENTIFIER().getText(),
                                          "scope_longname": self.packageName + '.'+ scope_longname,
                                          "scope_parent": scope_parents[-2] if len(scope_parents) >= 2 else None,
                                          "scope_contents": self.extract_original_text(ctx),
                                          "scope_modifiers": list(reversed(self.modifiers)),
                                          "line": line,
                                          "col": col[:-1],
                                          "type_ent_longname": self.packageName + '.' +self.class_Name, 'File' : self.FileName,
                                          'is_overrided' : self.isoverride, 'referenced': self.referencekind , 'modifiersx': string + ' ' + 'Method' }


            self.modifiers = []


    def exitMethodDeclaration(self, ctx:JavaParserLabeled.MethodDeclarationContext):
        self.dic = {}
        self.isoverride = False
        self.modifiers = []


    def exitclassDeclaration(self , ctx: JavaParserLabeled.ClassDeclarationContext):

        self.class_Name = ""
        self.extendedname = ""
        self.extend_word = False
        self.context = ""
        self.parent = []

    def enterClassBodyDeclaration2(self, ctx:JavaParserLabeled.ClassBodyDeclaration2Context):
        children = ctx.children

       
        for x in children:
         
            if type(x).__name__ == 'ModifierContext' :
                self.modifiers.append(x.getText())

                if type(x.children[0]).__name__ != 'TerminalNodeImpl':
                    y = x.children[0]
                    if type(y.children[0]).__name__  == 'AnnotationContext' :
                            self.modifiers.remove(x.getText())




            if type(x).__name__ ==    'MemberDeclaration0Context'  :
                if type(x.children[0]).__name__  != 'MethodDeclarationContext':
                       
                         self.modifiers = []
            elif type(x).__name__ != 'MemberDeclaration0Context' and type(x).__name__ !=  'ModifierContext':
                    self.modifiers = []




    def enterClassOrInterfaceType(self, ctx: JavaParserLabeled.ClassOrInterfaceTypeContext):

        if self.extend_word  and self.isclass :
            self.extendedname = ctx.IDENTIFIER()[0].getText()
            key1 = self.packageName + '.'+self.class_Name

            if self.extendedname in self.Imports :
                key2 = self.Imports[self.extendedname]
            else:
                key2 = self.packageName + '.' + self.extendedname

            self.extendedlist.append((key1,key2))
            self.extend_word = False
            self.isclass = False
            self.referencekind = key2

    def enterPackageDeclaration(self, ctx: JavaParserLabeled.PackageDeclarationContext):
        self.packageName = ctx.qualifiedName().getText()


    def enterFieldDeclaration(self, ctx:JavaParserLabeled.FieldDeclarationContext):
        self.modifiers = []


    def enterTypeTypeOrVoid(self, ctx:JavaParserLabeled.TypeTypeOrVoidContext):
       if( type(ctx.parentCtx).__name__ == 'MethodDeclarationContext'):
            x = ctx.children[0]
            if type(x).__name__ == 'TypeTypeContext':
                    t = x.children[0]
                    tt = t.children[0]
                    self.Methodkind = tt.getText()

            else:
                self.Methodkind = x.getText()


           
            self.dic['Methodkind'] = self.Methodkind
            
            self.classes[self.dic['type_ent_longname']].append(self.dic)

    def enterAnnotation(self, ctx:JavaParserLabeled.AnnotationContext):
        if ctx.qualifiedName().getText() == 'Override':
            self.isoverride = True
            




def get_parse_tree(file_path):
    file = FileStream(file_path)
    lexer = JavaLexer(file)
    tokens = CommonTokenStream(lexer)
    parser = JavaParserLabeled(tokens)
    return parser.compilationUnit()

def main():


    path = "C:/Users/Asus/PycharmProjects/pythonProject1/benchmark/105_freemind/src/main/java/accessories/plugins/BlinkingNodeHook.java"
    tree = get_parse_tree(path)
    listener = overridelistener()
    walker = ParseTreeWalker()
    walker.walk(listener, tree)
    print(listener.get_classes)
    print('db',listener.get_db)



if __name__ == '__main__':
    main()
