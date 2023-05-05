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

[How to run IsobaricQuant](https://isobaricquant.readthedocs.io/en/latest/#how-to-run-isobaricquant)

## Documentation Update: Using msconvert with Docker for IsobaricQuant

In order to properly process your data with IsobaricQuant, you will need to generate an mzML file from your raw mass spectrometry data file. This can be achieved using ProteoWizard's msconvert tool, which is available as a Docker image. This guide will walk you through the process of using the msconvert Docker image to convert your raw data file into an mzML file.

### Prerequisites

1. Install [Docker](https://docs.docker.com/get-docker/) if you haven't already.
2. Ensure you have the raw mass spectrometry data file(s) that you want to convert.

### Step-by-Step Guide

1. Pull the msconvert Docker image:
    ```
    docker pull chambm/pwiz-skyline-i-agree-to-the-vendor-licenses
    ```
    This command downloads the Docker image containing msconvert and its dependencies.

2. Create a directory to store the mzML output files:
    ```
    mkdir output
    ```
    
    This will create a new directory named "output" in your current directory, where the converted mzML files will be stored.

3. Run the msconvert Docker container to convert your raw file:
    ```
    sudo docker run -it --rm -e WINEDEBUG=-all -v /path/to/your/raw/files:/data -v /path/to/output/directory:/output chambm/pwiz-skyline-i-agree-to-the-vendor-licenses wine msconvert /data/your_raw_file.raw --mzML --simAsSpectra --32 --zlib --filter "peakPicking true 1-" --filter "zeroSamples removeExtra" -o /output --outfile your_mzML_file.mzML
    ```

Replace `/path/to/your/raw/files` with the path to the directory containing your raw mass spectrometry data files, and `/path/to/output/directory` with the path to the "output" directory you created in step 2. Replace `your_raw_file.raw` with the name of your raw file.

This command will run the Docker container, mount the appropriate input and output directories, and execute msconvert with the specified filters and options. The generated mzML file will be saved in the "output" directory with the name `your_mzML_file.mzML`.

### Notes on the msconvert Filters and Options

The following filters and options are recommended for use with IsobaricQuant:

- `--mzML`: This option specifies that the output file format should be mzML.
- `--simAsSpectra`: This option ensures that selected ion monitoring (SIM) scans are treated as spectra.
- `--32`: This option sets the output file's binary encoding to 32-bit.
- `--zlib`: This option compresses the output file using zlib.
- `--filter "peakPicking true 1-"`: This filter performs peak picking on the data, retaining only the most intense peaks.
- `--filter "zeroSamples removeExtra"`: This filter removes zero-intensity samples, reducing the size of the mzML file.

You can modify these filters or options based on your specific requirements. For a comprehensive list of available filters and their descriptions, consult the [ProteoWizard documentation](http://proteowizard.sourceforge.net/tools/filters.html).

After generating the mzML file(s), you can proceed with the IsobaricQuant workflow as outlined in the repository's documentation.




    