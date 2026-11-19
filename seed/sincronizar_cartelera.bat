@echo off
rem Trae la cartelera de Eventive (solo lectura) y la publica en Firebase Hosting.
rem Lo programa el Programador de tareas de Windows (ver README). La clave secreta sale de local.properties.
cd /d "%~dp0\.."
for /f %%d in ('powershell -NoProfile -Command "(Get-Date).AddDays(-14).ToString('yyyy-MM-dd')"') do set DESDE=%%d
rem Argumentos extra (p. ej. --incluir-pruebas para la demo) van a desde_eventive.py. La tarea programada no pasa ninguno.
python -I seed\desde_eventive.py --salida hosting\public\cartelera.json --desde %DESDE% %* || exit /b 1
call firebase deploy --only hosting || exit /b 1
echo Cartelera sincronizada desde %DESDE%.
