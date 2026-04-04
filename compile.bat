@echo off
echo ============================================
echo  Compilando Corretora Distribuida RMI...
echo ============================================

if not exist out mkdir out

javac -encoding UTF-8 -d out src\*.java

if %ERRORLEVEL% equ 0 (
    echo.
    echo [OK] Compilacao concluida com sucesso!
    echo Arquivos gerados em: out\
) else (
    echo.
    echo [ERRO] Falha na compilacao. Verifique os erros acima.
)
pause
