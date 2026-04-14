Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
jarPath = fso.GetParentFolderName(WScript.ScriptFullName) & "\out\FSROT_GUI.jar"
WshShell.Run "javaw -jar """ & jarPath & """", 0, False
