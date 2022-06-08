# Welcome to IsobaricQuant


IsobaricQuant, a Java-based software tool for the quantification, visualization, and filtering of isobarically-labeled peptides. IsobaricQuant is a cross-platform standalone tool that can be operated via an intuitive graphical user interface (GUI), or integrated into custom pipelines via command line. For input, it requires the mzML file of the MS run, a CSV file containing peptide spectral matches (PSM) obtained from a search engine, and a user-supplied text configuration file. Optionally, isotopic impurity correction can be performed using a reporter ion isotopic distribution CSV file. IsobaricQuant supports MS2 and MS3 level reporter ion quantification for iTRAQ up to 8-plex, TMT up to 11-plex and TMTpro up to 16-plex. An integrated viewer allows visual inspection of isolation window purity in MS1 scans, reporter ion quantification, and fragment ion assignment. For MS3 acquisition methods, it further enables visual assessment of SPS ion selection.

## Documentation

The full documentation for IsobaricQuant can be found at our [online documentation](https://isobaricquant.readthedocs.io/en/latest/)

## Installation

In order to run ``IsobaricQuant`` Java 17 is needed, first make sure you have the correct version of java installed with the following command on your terminal:

    java --version

If you don't have version 17 installed, please refer to our extended documentation for further steps:

[Dependencies](https://isobaricquant.readthedocs.io/en/latest/#dependencies)

If you have Java 17 installed, congratulations! Now you can proceed to download the latest version of IsobaricQuant jar file for your platform:
- [Windows](https://github.com/Villen-Lab/isobaricquant/releases/download/v1.0.1/IsobaricQuant_WIN.jar)
- [MacOS](https://github.com/Villen-Lab/isobaricquant/releases/download/v1.0.1/IsobaricQuant_MACOS.jar)
- [Linux](https://github.com/Villen-Lab/isobaricquant/releases/download/v1.0.1/IsobaricQuant_LINUX.jar)

Once you downloaded your jar file, place it in a folder of your choice, now you are ready to run IsobaricQuant, for further information, please refer to the documentation:

[Set-up](https://isobaricquant.readthedocs.io/en/latest/#installation)

## Quickstart

After downloading the jar file, ``IsobaricQuant`` can be used via GUI: 
    
    java --add-opens java.base/java.lang=ALL-UNNAMED -jar IsobaricQuant.jar

Or it can be used via command line:

    java -jar IsobaricQuant.jar -c <config_file> -mzf <mz_file> -h <hits_file> -o <output_folder>

For file input and output description please refer to the documentation:

[How to run IsobaricQuant](https://isobaricquant.readthedocs.io/en/latest/#io_files)

    