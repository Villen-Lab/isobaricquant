Algorithm
=========

   For every peptide hit of the input file submitted, we get the
   corresponding scan (MS2 or MS3 depending on scan level requested) and
   search for every label the nearest peak to their mz applying a window
   (+/- PPMTolerance). The peak selection is done using the
   [searchMethod] indicated by the user.

   If the user selected the option to recalculate the intensities based
   on the datasheet provided, then it will recalculate them using the
   corresponding equation matrix with the observed intensities. If we
   haven’t obtained enough data to solve the matrix, then it will use
   the observed intensities instead.

Noise
-----

   Removes the intensities from the peaks of the scan that are within
   the labels (low and high mz), and the noise is the average of the
   remaining peaks intensities. The noise is calculated by obtaining all
   the intensities within the lowest mz label (- PPMTolerance applied) -
   1, and the highest one (+ PPMTolerance applied) + 1, and then
   removing the observed intensities of the labels. The noise will be
   the average of these intensities.

Excluded Peaks
--------------

For all score calculations in MS2, the following peaks are excluded:

* MS1 non-fragmented precursor M+H
* TMT reporter ions (except for msnTotalSignal)
* TMTpro complementary ion clusters

    * Main series [(M+H) - 163] to [(M+H) - 151]
    * Second series [(M+H) - 180] to [(M+H) - 168]
    * Third series [(M+H) - 134] to [(M+H) - 122]

* TMT (6/10/11-plex) complementary ion clusters

    * Main series [(M+H) - 160] to [(M+H) - 151]
    * Second series [(M+H) - 177] to [(M+H) - 168]

In many scans, we observed TMT complementary ion clusters to be highly abundant, but without much predictive value, especially for SPS score calculations, as they are excluded from SPS ion selection as well.


MSN Scores
==========

ScoreType.ISOTOPE_DISTRIBUTION
------------------------------


   Gets the precursor for the scan, and calculates a mz window based on
   ppm tolerance for MS1 and +/- 1. This window is used to calculate the
   isotopic distribution (mzs). For all peaks in the scan, the
   intensities are accumulated in 2 different groups: isobIntensities
   for all the intensities of peaks that are within the isotopic
   distribution peaks, and nonIsobIntensities for the ones that are not
   in the distribution.

The score is calculated as:

::

   score = isobIntensities/(isobIntensities + nonIsobIntensities)

ScoreType.REPORTERS_INTENSITY
-----------------------------

   Gets the higher and lower mz from all the labels and for every peak
   of the scan (MS2 or MS3), accumulates the intensities that are within
   this range (labels range).

Using the isobaric intensities of the labels, the score is calculated
as:

::

   score = isobIntensities/allIntensities

ScoreType.REPORTERS_FOUND
-------------------------

   Counts how many of the isobaric labels are within the scan peaks
   (using MS2 or MS3 tolerance), and calculates the score as:

::

   cnt/isoLabels.size()

Precursor signal percentage
---------------------------

   Intensity of the precursor percentage from the total intensity in a
   given window (MS1). Gets the precursor intensity using the mz +/-
   MS1PPMTolerance. We limit the mz window to search for intensities by
   using the lowest precursor mz - ms1 window in Da and the highest +
   window in Da.

|  Accumulates all peaks intensities within the specified mz window of
  the MS1 scan.
|  Calculates the signal as:

::

   signalPerc = precIntensity/allIntensities

Precursor weighted signal
-------------------------


   Using the previous MS1 scan to the precursor’s, the precursor’s MS1,
   and the posterior one. We calculate the retention time of each scan
   and then the total distance between the scans as: Precursor’s
   retention time + Distance between previous MS1 and precursor’s +
   distance between precursor’s and posterior MS1. For each of these 3
   scans we calculate the precursor signal percentage so that the
   weighted score can be calculated as:

::

   (dPivotCurrent / totalDistances) * currentSignal + (dPivotPre / totalDistances) * preSignal + (dPivotPost / totalDistances) * postSignal

MS2 scores
==========

All MS2 scores are calculated by matching the BY fragment ions of the
peptide to their most similar peak. (BY Fragment ions for the peptide,
BY fragment ions for the peptide with neutral loss applied (calc all
fragment ions for peptide mass - neutral mass loss
[neutralLossesForFragmentIons]), and neutral losses specified
[neutralLosses]) From the peak list we are excluding the MS1 precursor,
the reporter ions, and the complement ions.

**top X:** top x peaks with highest intensity.

Peptide Intensity Score
-----------------------

From Scan peaks, sum of intensities of fragment ions divided by total.

::

   score = totalFragmIonsIntensity/totalIntensity

Top X Peptides Peaks Ratio Top X defined by user Number of fragment ions
in top X divided by top X topXFIons/topXNum

Peptide Top X Intensity Score
-----------------------------

Sum of intensities of fragment ions in top X divided by the total sum of
intensities of top X.

::

   score = topXFragmIonsIntensity/topXIntensity

Top X Intensity From Total Score
--------------------------------

Sum of intensities in top X divided by the sum of all peaks’
intensities.

::

   score = topXIntensity/totalIntensity

Peptide Top X Intensity From Total Score
----------------------------------------

Sum of intensities of fragment ions in top X divided by the sum of all
peaks’ intensities.

::

   score = topXFragmIonsIntensity/totalIntensity

Top Peak Intensity Score
------------------------

Higher intensity divided by the sum of all peaks’ intensities.

::

   score = topPeakIntensity/totalIntensity

Top Peak Intensity Top X Score
------------------------------

Highest intensity divided by the total sum of intensities of top X.

::

   score = topPeakIntensity/topXIntensity

Top Peak From Peptide
---------------------

Indicates if the top peak (most intense) is a fragment ion

Top Peak From Peptide Neutral Loss
----------------------------------

Indicates if the top peak (most intense) is a neutral loss

Top Peak Mass
-------------

Mass of the top peak (most intense)

msnTotalSignal
--------------

Sum of all the isobaric quant intensities (found labels)

precTotalSignal
---------------

Sum of precursor intensities found in MS2 (low and high mz is calculated
using MS2PPMTolerance)

totalSignalSPSWind
------------------

Sum of precursors intensities found in MS2 within a window of mz, this
window is calculated as follows:

::

   mz +/- ms2PrecursorWindowTolerance(mz)

precTPIntRatio
--------------

::

   score = precTotalSignal/totalSignalSPSWind

precRepIntRatio
---------------

::

   matchingBYInt = matching B and Y fragment ions (from the SPS list) intensities
   score = matchingBYInt / totalSignalSPSWind

precTPNumRatio
--------------

numPeaksSPSWind = number of peaks found in MS2 within a window of mz,
this window is calculated as follows:

::

   mz +/- ms2PrecursorWindowTolerance(mz)
   score = number of precursors found / numPeaksSPSWind

MS3 scores
==========

Definitions
-----------

Precursors
~~~~~~~~~~

List of precursors provided by the scan minus their parent scan
precursors MS3 <- we are removing the precursors that are within the MS2
and MS1 MS2 <- we are removing the precursors that are within the MS1

Precursors intensities
~~~~~~~~~~~~~~~~~~~~~~

We are obtaining the intensities of the MS3 precursors in the MS2 (peak
selection using MS2PPMTolerance window)


Peptide Fragment ions
~~~~~~~~~~~~~~~~~~~~~

B and Y fragment ions for the peptide (search hit) matching MS2 peaks.

Precursors matching BY intensities
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Using the fragment ions calculated for the peptide (search hit). Sum of
all fragment ions intensities that their mz is found within any of the
precursors windows (using ``MS2PPMTolerance``)

Precursors window intensities
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

| For every MS3 precursor we are obtaining all intensities within the
  window defined by
| ``lowMz = (Precursor mz - tolerance)`` with ``MS2PPMTolerance``
| ``highMz = (Precursor mz + tolerance)`` with ``MS2PPMTolerance``
| Where tolerance is the conversion from ``ms2PrecursorWindowTolerance``
  mass to ``m/z`` using the peptide charge (**search hit!**)


MSN Total signal
----------------

Sum of all label intensities found in the scan (level defined in the
conf file) The peak is found within a window (PPMTolerance)

Precursor total signal
----------------------

Sum of precursors intensities

Total signal SPS window
-----------------------

Sum of precursors window intensities

precTPIntRatio
--------------

Precursor total signal divided by total signal sps window


precRepIntRatio
---------------

Precursors matching BY intensities divided by total signal SPS window


precTPNumRatio
--------------

Number of MS3 precursors found in MS2 divided by number of peaks found
in their windows.