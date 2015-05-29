@echo off

rem usage: restart <PID>

rem How to find a PID for a given program:
rem tasklist /fo csv | findstr /i "snap-desktop.exe"

:loop
for /F "tokens=2" %%i in ('tasklist') do (
    if "%%i" equ "%1" (
		timeout /T 1 /NOBREAK
		goto loop
	)
)

cd "${installer:sys.installationDir}"
call .\bin\${compiler:snapDesktopName}


