# Report Name Check Tool

A tool to confirm and correct the naming scheme of sub-site and apendix B reports, comparing against a location hierarchy spreadsheet.

## Table of Contents

- [Setup](#setup)

- [GUI and How To Use](#gui-and-how-to-use)
  
  - [Hierarchy sheet](#hierarchy-sheet)
  
  - [Correct naming](#correct-naming)

- [Troubleshooting](#troubleshooting)

- [Details](#details)
  
  - [Find Reports](#find-reports)
  
  - [Check Naming](#check-naming)
  
  - [Incorrect Files](#incorrect-files)

- [Changing the code](#changing-the-code)
  
  - [Externalized Strings](#externalized-strings)
  
  - [Java Code](#java-code)

- [In the GitHub](#in-the-github)

- [License](#license)
  
  - [Notice](#notice)

## Setup

Java SE 14 or newer is required to run this program. If you've used any of my previous tools, you'll already have it installed. If you don't have Java 14 or newer, you can download an installer for Temurin/OpenJDK 21 (the newest LTS version, backwards-compatible) [here](https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.4%2B7/OpenJDK21U-jdk_x64_windows_hotspot_21.0.4_7.zip). This is an open-source version of jav. Once downloaded, you can run the installer by double-clicking, it will open a window guiding you through the installation. Leaving everything as the defaults and just clicking through should work perfectly.

The program itself can be downloaded from the [GitHub](https://github.com/Jaden-Unruh/Report-Name-Check/), the latest build can be found in the `Releases` section on the right of the page. I advise against using any pre-release versions I've put on there, they will likely have bugs and other undesired behaviors. Clicking on the release marked `Latest` will take you to the release page, which has patch notes and, at the bottom, a list of assets. Click the `.jar` file, and it will download. You can rename the file to whatever you like after it's downloaded, but by default it will have a systematic name like `Report-Name-Check-Tool-1.2.1.jar`.

Once Java and the program `.jar` are installed, double-click the `.jar` to run.

## GUI and How To Use

After double-clicking the `.jar`, a window titled "Report Name Check Tool" will open. It will have two prompts, as described below:

1. `Location Hierarchy Spreadsheet: Select...`
   
   * Click on the select button to open a file prompt, navigate to and select a location hierarchy spreadsheet (`...Location Hierarchy Details...xlsx`). Note that this must be a `*.xlsx` file, rather than `*.xlsb` or any other spreadsheet filetype - see Troubleshooting for more. The contents of this spreadsheet should be as described below.

2. `Parent folder for reports: Select...`
   
   * Click on the select button, and, as above, select the parent folder for all the reports you'd like to run the script on. They can be in sub-folders within sub-folders, as long as they all trace their parentage to the selected folder.

The other contents of the window are a text area titled `Selected folder contents`, and a row of buttons. Once you select a parent folder, the text area will update to show the first level of contents within that folder, so you can confirm you've selected the right folder.

The buttons - close closes the program; help opens a small help window with a link to this GitHub to view the readme; open selected folder opens the selected parent folder in windows file explorer, and run starts the program.

### Hierarchy sheet

The location hierarchy sheet should contain the details of all the reports you're naming. The relevant columns of the sheet should be as follows: Site ID in column C, Site Description in column D, Location ID in column E, Maximo ID in column H.

### Correct naming

The reports, when named correctly, will match the following schemes:

Sub-site report: Year_Site ID_Location ID_Maximo Sub Top Level #_FCA Sub-Site Report_Site Description

i.e. 2024_IA000_A00-00_AB900000_FCA Sub-Site Report_Akana University

App B report: Year_Site ID_Location ID_Maximo Sub Top Level #_FCA Sub-Site Report App B_Site Description

In [regex](https://en.wikipedia.org/wiki/Regular_expression), this can be expressed as `20\d{2}_(?:IE|IA|JS)\d{3}_[A-Z]\d{2}-\d{2}_AB\d{6}_FCA Sub-Site Report(?: App B)?_.+`

## Troubleshooting

> Nothing's happening when I double-click the `.jar` file

Ensure you've installed Java as specified under [Setup](#setup). If you believe you have, try checking your java version:

1. Press the Windows and R keys, type `cmd` and press enter - this will open a command prompt window

2. Type `java -version` and press enter

3. If you've installed java as specified, the first line under your typing should read `openjdk version "21.0.4" 2024-07-17` or something similar (the program requires a minimum of version 14). If, instead, it says `'java' is not recognized as an internal...` then java is not installed

---

> I only have spreadsheets of type `.xlsb` or `.cxv` (or any other spreadsheet type) and the program won't open them

Open the spreadsheets in Microsoft Excel and select `File -> Save As -> This PC` and choose `Excel Workbook (.xlsx)` from the drop-down. A full list of filetypes that Excel supports (and thus can be converted to .xlsx) can be found [here](https://learn.microsoft.com/en-us/deployoffice/compat/office-file-format-reference#file-formats-that-are-supported-in-excel).

---

> `Run` isn't doing anything

Ensure you've selected a hierarchy spreadsheet and a parent directory. The spreadsheet must be of type `.xlsx`.

---

> I'm getting an error message popping up when I run the program

If yo'ure getting an error message and you can't figure out what it's saying or how to fix it, reach out to me. If you click `More Info` on the error popup and copy the big text box, that text (a full [stack trace](https://en.wikipedia.org/wiki/Stack_trace) of the error) can help me figure out what's going on.

---

> Something else is going wrong

Don't hesitate to reach out to me if you have any other issues - always happy to help.

## Details

There are a few main sections the program runs - finding all the reports, seeing which reports are named incorrectly, and then prompting the user to help rename any incorrect reports.

### Find reports

The program [recursively](https://en.wikipedia.org/wiki/Recursion_(computer_science)) finds all `.pdf` files within the selected directory, and adds them all to a large set

### Check naming

The program compares the name of each pdf to its regular expression of what a name should look like. If it matches, it will pull all the data from the name and check it against the hierarchy spreadsheet. If all still matches, and the description doesn't contain any prohibited characters, it moves on. If just the description is wrong, or contains erroneous characters, it will prompt the user for an updated description, and, if provided, will rename the file and move on.

If any other data is wrong, or the user skips the description prompt, the file will be added to a set of incorrectly named files to be further processed.

### Incorrect files

For any incorrect files, the program will pull what data it can from the file name and the hierarchy spreadsheet, and will present that data to the user. The user will then copy any information they deem correct from what the program has gathered, and provide any other information, then the program will compile that into a correct name (which is guaranteed to meet the regex), and rename the file.

## Changing the code

The `.jar` file is compiled and compressed, meaning all the code is not human-readable. You can decompress and recompress the file to change certain parts, like some of the GUI text, but all of the code itself is not editable. Instead, all of the program files are included in this [GitHub repository](https://github.com/Jaden-Unruh/Report-Name-Check/) so that anyone other than me could download them and open them in an IDE.

### Externalized Strings

One of the main components of the code you could edit without recompiling are the externalized strings - nearly every user-visible piece of text in the GUI and that the program produces in the info file, for example, are in a (somewhat) user-editable file. You can decompress the `.jar` using a tool like [WinRar](https://www.win-rar.com/).

The externalized Strings are found in `report-name-check-x.x.x.jar\reportNameCheck\messages.properties`. Each is one line, constructed as a key-value pair, where everything to the left of the '=' is the key, used by the program to find the String to use (don't change that side), and everything to the right can be edited to change what the program uses whenever it references that key.

### Java Code

The actual code for the project, written in Java, cannot be edited without recompiling the project. Thus, I have provided all my project files in the GitHub repository. The code itself is located within [/src/main/java/reportNameCheck](https://github.com/Jaden-Unruh/Report-Name-Check/tree/master/src/main/java/reportNameCheck), with `Main.java` being the entry program file. To edit these, I would advise cloning the project from GitHub and opening in your preferred IDE, then re-building using a tool like [Maven](https://maven.apache.org/). I have included my `pom.xml` to facilitate the build.

## In the GitHub

The github repository has a handful of files, but most of them are only necessary if you wish to modify the code.

If you're just running the program, get the `.jar` from the releases section on the right side of the page.

If you're curious for a look under the hood, the java code (the interesting bits) can be viewed by clicking through `src/main/java/reportNameCheck`, and `Main.java` is the biggest and most interesting class file.

Other notable files - `doc` contains documentation in [javadoc](https://docs.oracle.com/javase/8/docs/technotes/tools/windows/javadoc.html), which can be useful for editing and working with the code. I've been as thorough as possible, documenting private methods and fields, so that a future developer isn't left guessing. `pom.xml` contains somewhat-human-readable information that [Maven](https://maven.apache.org/) uses to compile the project.

## License

Many of my previous tools were released without a license, which is, legally, very restrictive. For that reason, this tool is released with a license. It shouldn't affect any use of the tool within Akana, and doesn't have any impact on the copyright of data edited by the code - only future distributions of the code itself.

Report Name Check is available under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html) or later. In summary, this code is available to use, copy, and modify, under the condition that all derivative works containing the code (not including files edited by the code) are released under the same license. This project is provided without liability or warranty. See the `LICENSE` for more.

### Notice

In this project, I used libraries from a few sources, including some from [Apache](https://www.apache.org/). Although freely available to use under the Apache License, section 4(d) of that license requires that the following Notice be included in the documentation:

```text
=========================================================================
==  NOTICE file corresponding to section 4(d) of the Apache License,   ==
==  Version 2.0, in this case for the Apache XmlBeans distribution.    ==
=========================================================================

This product includes software developed at
The Apache Software Foundation (http://www.apache.org/).

Portions of this software were originally based on the following:
  - software copyright (c) 2000-2003, BEA Systems, <http://www.bea.com/>.
Note: The ASF Secretary has on hand a Software Grant Agreement (SGA) from
BEA Systems, Inc. dated 9 Sep 2003 for XMLBeans signed by their EVP/CFO.

Aside from contributions to the Apache XMLBeans project, this
software also includes:

  - one or more source files from the Apache Xerces-J and Apache Axis
  products, Copyright (c) 1999-2003 Apache Software Foundation

  - W3C XML Schema documents Copyright 2001-2003 (c) World Wide Web
  Consortium (Massachusetts Institute of Technology, European Research
  Consortium for Informatics and Mathematics, Keio University)

  - resolver.jar from Apache Xml Commons project,
  Copyright (c) 2001-2003 Apache Software Foundation
```

   
