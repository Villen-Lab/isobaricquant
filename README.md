# IsobaricQuant

## Getting started

Download [latest version](https://gitlab.com/public_villenlab/isobaricquant/-/releases/v1.0.0)

### Requirements

```Java 17```

### Command

```
java --add-opens java.base/java.lang=ALL-UNNAMED -jar IsobaricQuant.jar
```

# Algorithm
<div align="justify">
For every peptide hit of the input file submitted, we get the corresponding scan (MS2 or MS3 depending on scan level requested) and search for every label the nearest peak to their mz applying a window (+/- PPMTolerance). The peak selection is done using the [searchMethod] indicated by the user.

If the user selected the option to recalculate the intensities based on the datasheet provided, then it will recalculate them using the corresponding equation matrix with the observed intensities. If we haven’t obtained enough data to solve the matrix, then it will use the observed intensities instead.
</div>

# Noise
<div align="justify">
Removes the intensities from the peaks of the scan that are within the labels (low and high mz), and the noise is the average of the remaining peaks intensities.
The noise is calculated by obtaining all the intensities within the lowest mz label (- PPMTolerance applied) - 1, and the highest one (+ PPMTolerance applied) + 1, and then removing the observed intensities of the labels. The noise will be the average of these intensities.
</div>

# MSN Scores

## ScoreType.ISOTOPE_DISTRIBUTION
<div align="justify">
Gets the precursor for the scan, and calculates a mz window based on ppm tolerance for MS1 and +/- 1. This window is used to calculate the isotopic distribution (mzs). 
For all peaks in the scan, the intensities are accumulated in 2 different groups: isobIntensities for all the intensities of peaks that are within the isotopic distribution peaks, and nonIsobIntensities for the ones that are not in the distribution.  
</div>

The score is calculated as:  

```
score = isobIntensities/(isobIntensities + nonIsobIntensities)
```


## ScoreType.REPORTERS_INTENSITY
<div align="justify">
Gets the higher and lower mz from all the labels and for every peak of the scan (MS2 or MS3), accumulates the intensities that are within this range (labels range).
</div>

Using the isobaric intensities of the labels, the score is calculated as:  


```
score = isobIntensities/allIntensities
```

## ScoreType.REPORTERS_FOUND
<div align="justify">
Counts how many of the isobaric labels are within the scan peaks (using MS2 or MS3 tolerance), and calculates the score as:
</div>
<br>

```
cnt/isoLabels.size()
```

## Precursor signal percentage
<div align="justify">
Intensity of the precursor percentage from the total intensity in a given window (MS1).
Gets the precursor intensity using the mz +/- MS1PPMTolerance. We limit the mz window to search for intensities by using the lowest precursor mz - ms1 window in Da and the highest + window in Da.
</div>
<br>
Accumulates all peaks intensities within the specified mz window of the MS1 scan.  
<br>
Calculates the signal as:  

```
signalPerc = precIntensity/allIntensities
```

## Precursor weighted signal
<div align="justify">
Using the previous MS1 scan to the precursor’s, the precursor’s MS1, and the posterior one. We calculate the retention time of each scan and then the total distance between the scans as:
Precursor’s retention time + Distance between previous MS1 and precursor’s + distance between precursor’s and posterior MS1.
For each of these 3 scans we calculate the precursor signal percentage so that the weighted score can be calculated as:
</div>
<br>

```
(dPivotCurrent / totalDistances) * currentSignal + (dPivotPre / totalDistances) * preSignal + (dPivotPost / totalDistances) * postSignal
```

# MS2 scores

All MS2 scores are calculated by matching the BY fragment ions of the peptide to their most similar peak. (BY Fragment ions for the peptide, BY fragment ions for the peptide with neutral loss applied (calc all fragment ions for peptide mass - neutral mass loss [neutralLossesForFragmentIons]), and neutral losses specified [neutralLosses])
From the peak list we are excluding the MS1 precursor, the reporter ions, and the complement ions.

**top X:** top x peaks with highest intensity.

## Peptide Intensity Score

From Scan peaks, sum of intensities of fragment ions divided by total.  

```
score = totalFragmIonsIntensity/totalIntensity
```

Top X Peptides Peaks Ratio
Top X defined by user
Number of fragment ions in top X divided by top X
topXFIons/topXNum

## Peptide Top X Intensity Score

Sum of intensities of fragment ions in top X divided by the total sum of intensities of top X.  

```
score = topXFragmIonsIntensity/topXIntensity
```

## Top X Intensity From Total Score

Sum of intensities in top X divided by the sum of all peaks’ intensities.  

```
score = topXIntensity/totalIntensity
```

## Peptide Top X Intensity From Total Score

Sum of intensities of fragment ions in top X divided by the sum of all peaks’ intensities.  

```
score = topXFragmIonsIntensity/totalIntensity
```

##  Top Peak Intensity Score

Higher intensity divided by the sum of all peaks’ intensities.  

```
score = topPeakIntensity/totalIntensity
```

## Top Peak Intensity Top X Score

Highest intensity divided by the total sum of intensities of top X.  

```
score = topPeakIntensity/topXIntensity
```

##  Top Peak From Peptide

Indicates if the top peak (most intense) is a fragment ion

##  Top Peak From Peptide Neutral Loss

Indicates if the top peak (most intense) is a neutral loss

## Top Peak Mass

Mass of the top peak (most intense)

## msnTotalSignal

Sum of all the isobaric quant intensities (found labels)

## precTotalSignal

Sum of precursor intensities found in MS2 (low and high mz is calculated using MS2PPMTolerance)

## totalSignalSPSWind

Sum of precursors intensities found in MS2 within a window of mz, this window is calculated as follows:  

```
mz +/- ms2PrecursorWindowTolerance(mz)
```

## precTPIntRatio

```
score = precTotalSignal/totalSignalSPSWind
```

## precRepIntRatio

```
matchingBYInt = matching B and Y fragment ions (from the SPS list) intensities
score = matchingBYInt / totalSignalSPSWind
```

## precTPNumRatio

numPeaksSPSWind = number of peaks found in MS2 within a window of mz, this window is calculated as follows:  

```
mz +/- ms2PrecursorWindowTolerance(mz)
score = number of precursors found / numPeaksSPSWind
```

# MS3 scores

## MSN Total signal
Sum of all label intensities found in the scan (level defined in the conf file)
The peak is found within a window (PPMTolerance)

### Precursors

List of precursors provided by the scan minus their parent scan precursors
MS3 <- we are removing the precursors that are within the MS2 and MS1
MS2 <- we are removing the precursors that are within the MS1

### Precursors intensities

We are obtaining the intensities of the MS3 precursors in the MS2 (peak selection using MS2PPMTolerance window)

## Precursor total signal

Sum of precursors intensities

## Precursors window intensities

For every MS3 precursor we are obtaining all intensities within the window defined by  
```lowMz = (Precursor mz - tolerance)``` with ```MS2PPMTolerance```  
```highMz = (Precursor mz + tolerance)``` with ```MS2PPMTolerance```  
Where tolerance is the conversion from ```ms2PrecursorWindowTolerance``` mass to ```m/z``` using the peptide charge (**search hit!**)  

## Total signal SPS window

Sum of precursors window intensities

## precTPIntRatio

Precursor total signal divided by total signal sps window

### Peptide Fragment ions

B and Y fragment ions for the peptide (search hit) matching MS2 peaks.

### Precursors matching BY intensities

Using the fragment ions calculated for the peptide (search hit). Sum of all fragment ions intensities that their mz is found within any of the precursors windows (using ```MS2PPMTolerance```)

## precRepIntRatio

Precursors matching BY intensities divided by total signal SPS window

## precTPNumRatio

Number of MS3 precursors found in MS2 divided by number of peaks found in their windows.

# Config file parameters

Zucchini parameter name / standalone parameter name

**method / isoMethod:** Isobaric Methods  
**Values:** iTRAQ4plex,iTRAQ8plex,TMTDuplex,TMT6plex,TMT10plex,TMT11plex,TMTpro  
**Default:** iTRAQ4plex  

**mass / MassType:** mass type  
**Values:** HCD fragmentation,Neutral mass  
**Default:** HCD fragmentation  

**scanLevel / scanLevel:** Scan level for isobaric quantification  
**Values:** MS2,MS3  
**Default:** MS2  

**ScoreType / scoreType:** Score calculation  
**Values:**  
Isotope distribution: peptide isotopes intensity/total intensity in isotopes window.  
Reporters intensity: reporters intensity/total intensity in window.  
Reporters found: num. reporters found/num. Reporters  
**Default:** Reporters intensity  

**Labels / confLabels:** Labels to be searched in (use comma separated values. ex: 114,115,116,117). Write 'all' to search all of them. For TMT10plex, use label reagent: 126,127N,127C,128N,128C...  
**Values:** input by user  
**Default:** all  

**recalculate_intensities / recalcIntensities:** If activated, it will recalculate intensities using the product data sheet.  
**Default:** not activated  

**dataSheet / dataSheetName:** Product data sheet  
**Values:** TMT10plex_QI218066,TMT11_TL277832  
**Default:** TMT10plex_QI218066  

**search.ppm / PPMTolerance:** PPM Tolerance for MS3  
**Values:** input by user  
**Default:** 10  

**search.ppm1 / MS1PPMTolerance:** PPM Tolerance for MS1  
**Values:** input by user  
**Default:** 10  

**search.ppm2 / MS2PPMTolerance:** PPM Tolerance for MS2  
**Values:** input by user  
**Default:** 1000  

**precSignalWindow / ms1PrecWindowDaltons:** Dalton window that will be used to calculate the precursor signal percentage in the MS1 scan.  
**Values:** input by user  
**Default:** 1  

**search.fragment.window / searchFragmentWindowTolerance:** MS2 precursor window (Da)  
**Values:** input by user  
**Default:** 2.5  

**search.fragment.file / generateFragmentIonsFile:** Generates a file with all peptides' fragment ions.  
**Default:** not activated  

**hit.impurity / hitImpurity:**  
**Values:** master_scan,weighted_avg  
The precursor signal is calculated using the weighted signal or signal percentage  
If it’s not weighted avg, then it will be ```precursor int / intensities``` in isolation window (calculated using ```ms1PrecWindowDaltons```)   
```
signalPerc = precIntensity / allIntensities  
```
if it’s weighted, then it will calculate the same thing as the above but for the current ms1, its previous ms1 and its posterior ms1. The result is the weighted signal, calculated using the RT distance to the current ms1  
```
(dPivotCurrent / totalDistances) * currentSignal + (dPivotPre / totalDistances) * preSignal + (dPivotPost / totalDistances) * postSignal  
```
**Default:** master_scan  

**search.method / searchMethod:** Search method used to select a TMT reporter ion peak  
**Values:** most_intense, least_intense, lower_ppm_error  
**Default:** most_intense  

**ms1DepthSearch / ms1DepthSearch:** Number of MS1 scans depth to search for MS3 scans  
**Values:** input by user  
**Default:** 5  

**topX / topXNum:** Top X most intense ions used to calculate scores  
**Values:** input by user  
**Default:** 10  

**neutralLosses:** Comma separated neutral loss masses (ex: 97.977,97.995). The peak will be searched for these NL.  
**Values:** input by user, e.g. “63.98,18.01,17.03”  

**neutralLossesFI:** Comma separated neutral loss masses (ex: 97.977,97.995). The fragment ions will be searched for these NL.  
**Values:** input by user, e.g. “63.98,18.01,17.03”  


In standalone config file provided, also have these following settings:
```
"ms2NoiseWindowTolerance":1,
"modifications":{
"varMods":"15.9949146202 M 42.01056468472 prot_n",
"consMods":"pep_n 304.20714532, C 57.02146372118, K 304.20714532",
"varTermParams":""},
"mzFilePath":"./mzFiles/file.mzML",
"productDataSheet":{
"id":1,
"userID":0,
"name":"TMTpro_dataSheet",
"creationDate":"2019-04-05 04:00:16.0"}
```
