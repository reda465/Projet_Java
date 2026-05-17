%{
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "symtable.h"
#include <math.h>

extern int yylex(void);
extern int yylineno;
extern char *yytext;
extern int num_colonne;

int nb_erreurs = 0;
void yyerror(const char *s);
%}

/* ── Tokens ─────────────────────────────────────────────── */
%token BEGIN_T
%token END
%token INT
%token WRITE
%token READ
%token WHILE
%token DO
%token OD
%token IF
%token THEN
%token ELSE
%token FI
%token FOR
%token TO
%token DONE
%token LPAREN
%token RPAREN
%token SEMICOL

%token ASSIGN
%token GT          /* >   */
%token LT          /* <   */
%token GE          /* >=  */
%token LE          /* <=  */
%token NE          /* !=  */
%token EQ          /* ==  */
%token SEQ         /* === */

%token PLUS
%token MINUS
%token MUL
%token DIV
%token MOD
%token POW

/* ── Union ──────────────────────────────────────────────── */
%union {
    int   num;
    char *str;
}

%token <num> NUM
%token <str> ID
%type  <num> expr

%nonassoc GT LT GE LE NE EQ SEQ
%left  PLUS MINUS
%left  MUL DIV MOD
%right POW

%start program

%%

program
    : BEGIN_T listinstr END
    | BEGIN_T error END
        {
            fprintf(stderr,"ERREUR SYNTAXIQUE Bloc principal mal forme.\n");
            yyerrok;
        }
    | error
        {
            fprintf(stderr,"ERREUR SYNTAXIQUE Le programme doit commencer par 'begin'.\n");
            yyerrok;
        }
    ;

listinstr
    : instr listinstr
    | instr
    ;

/* ── Instructions ───────────────────────────────────────── */
instr

    : INT ID
        { declare($2, yylineno); }

    | ID ASSIGN expr
        { set_value($1, $3, yylineno); }

    | WRITE expr
        { printf("%d\n", $2); }

   
    | READ LPAREN ID RPAREN
        { lookup($3, yylineno); }

    | WHILE LPAREN cond RPAREN DO listinstr OD

    
    | FOR ID ASSIGN expr TO expr DO listinstr DONE
        {
            int debut = $4;
            int fin   = $6;
            if (debut > fin) {
                fprintf(stderr, "[AVERTISSEMENT] Ligne %d : boucle for avec debut (%d) > fin (%d),"" corps non execute.\n",
                    yylineno, debut, fin);
            }
        
            set_value($2, fin, yylineno);
            fprintf(stderr,
                "[INFO] Boucle for '%s' : de %d a %d (variable mise a jour).\n",
                $2, debut, fin);
        }

    | IF LPAREN cond RPAREN THEN listinstr FI

    | IF LPAREN cond RPAREN THEN listinstr ELSE listinstr FI

    /* ── Récupération d'erreur par instruction ──────────── */
    | error
        {
            fprintf(stderr,
                "[ERREUR SYNTAXIQUE] Ligne %d, Token '%s' : "
                "instruction invalide, recuperation en cours...\n",
                yylineno, yytext);
            yyerrok;
        }
    ;

/* ── Expressions ────────────────────────────────────────── */
expr
    : expr PLUS  expr  { $$ = $1 + $3; }
    | expr MINUS expr  { $$ = $1 - $3; }
    | expr MUL   expr  { $$ = $1 * $3; }
    | expr DIV   expr
        {
            if ($3 == 0) {
                fprintf(stderr,
                    "[ERREUR SEMANTIQUE] Ligne %d : division par zero.\n",
                    yylineno);
                nb_erreurs++;
                $$ = 0;
            } else {
                $$ = $1 / $3;
            }
        }
    | expr MOD   expr
        {
            if ($3 == 0) {
                fprintf(stderr,
                    "[ERREUR SEMANTIQUE] Ligne %d : modulo par zero.\n",
                    yylineno);
                nb_erreurs++;
                $$ = 0;
            } else {
                $$ = $1 % $3;
            }
        }
    | expr POW   expr  { $$ = (int)pow($1, $3); }
    | MINUS expr       { $$ = -$2; }
    | ID               { $$ = lookup($1, yylineno); }
    | NUM              { $$ = $1; }
    | LPAREN expr RPAREN { $$ = $2; }
    | LPAREN error RPAREN
        {
            fprintf(stderr, "[ERREUR SYNTAXIQUE] Ligne %d : expression invalide entre parentheses.\n",
                yylineno);
            $$ = 0;
            yyerrok;
        }
    ;

/* ── Conditions ─────────────────────────────────────────── */
cond
    : expr GT  expr
    | expr LT  expr
    | expr GE  expr
    | expr LE  expr
    | expr NE  expr
    | expr EQ  expr
    | expr SEQ expr
    | error
        {
            fprintf(stderr,"[ERREUR SYNTAXIQUE] Ligne %d : condition invalide.\n",
                yylineno);
            yyerrok;
        }
    ;

%%

void yyerror(const char *msg)
{
    fprintf(stderr, "\n[ERREUR SYNTAXIQUE]\n"
        "  Ligne   : %d\n"
        "  Colonne : %d\n"
        "  Token   : '%s'\n"
        "  Message : %s\n\n",
        yylineno, num_colonne, yytext, msg);
    nb_erreurs++;
}

int main(void)
{
    printf("=== Debut de l'analyse ===\n\n");
    yyparse();

    printf("\n=== Fin de l'analyse ===\n");
    if (nb_erreurs == 0) {
        printf("Programme correct  (0 erreur)\n");
        return 0;
    }
    printf("Programme incorrect (%d erreur(s) detectee(s))\n", nb_erreurs);
    return 1;
}
