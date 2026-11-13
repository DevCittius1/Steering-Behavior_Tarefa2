@echo off
rem Compila e abre o jogo. Uso: rodar.bat        (abre no cenario 1)
rem                             rodar.bat 7      (abre direto no cenario 7)
cd /d "%~dp0"
dir /s /b src\*.java > fontes.txt
javac -encoding UTF-8 -d out @fontes.txt
if errorlevel 1 (
    del fontes.txt
    echo.
    echo Erro ao compilar. Confira se o JDK 21 esta instalado: rode "java -version".
    pause
    exit /b 1
)
del fontes.txt
if "%~1"=="" (
    java -cp out cars.engine.Window
) else (
    java -Dscenario=%~1 -cp out cars.engine.Window
)
