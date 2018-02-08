@echo off
title [CONFIG SEARCH]
cls
echo.
:find
set /p text="enter search text here: "
echo.
echo.search text "%text%" result:
findstr /I /N %text% *.xml
echo.
goto find