package edu.uw.villenlab.isobaricquant;


import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


/**
 * 2.2 recalculate using product data sheet 2.3 Matching fragment ions to output
 * and new scores
 *
 * @author allovet
 */
public class IsobaricQuant {

    /**
     * @param args the command line arguments 0: Instrument used 1: File name 2:
     * Search type 3: Configuration file id 4: Quantification id
     * @throws javax.xml.parsers.ParserConfigurationException
     */
    public static void main(String[] args) throws ParserConfigurationException, Exception {

        Options options = new Options();

        //Option modFile = new Option("m", "modfile", true, "Search modifications file path");
        Option qidgFile = new Option("q", "quantID", true, "Quantification ID number to differenciate quants");
        Option configFile = new Option("c", "configfile", true, "Configuration file path");
        Option mzFile = new Option("mzf", "mzfile", true, "mass spectroscopy file path. mzXML and mzML supported");
        Option dataSheetFile = new Option("s", "sheetFile", true, "Data Sheet file if recalcIntensities=true");
        Option hitsFile = new Option("h", "hitsFile", true, "Search hits input file (peptides). CSV, pepXML and mzIdentML supported");
        Option tokenOption = new Option("t", "token", true, "Token to name files");
        Option outputOption = new Option("o", "output", true, "Output directory");

        /*qidgFile.setRequired(false);
		 configFile.setRequired(true);
		 mzFile.setRequired(true);
		 dataSheetFile.setRequired(false);
		 hitsFile.setRequired(true);
		 tokenOption.setRequired(false);
		 outputOption.setRequired(true);*/
        qidgFile.setRequired(false);
        configFile.setRequired(false);
        mzFile.setRequired(false);
        dataSheetFile.setRequired(false);
        hitsFile.setRequired(false);
        tokenOption.setRequired(false);
        outputOption.setRequired(false);


        options.addOption(qidgFile);
        options.addOption(configFile);
        options.addOption(mzFile);
        options.addOption(dataSheetFile);
        options.addOption(hitsFile);
        options.addOption(tokenOption);
        options.addOption(outputOption);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);

            if (cmd.getOptions().length == 0) {
                Gui gui = new Gui();
                gui.launchGui(args);
            } else {

                //TODO CHECK required params
                String quantID = cmd.getOptionValue("quantID");
                String configFilePath = cmd.getOptionValue("configfile");
                String mzMLFilePath = cmd.getOptionValue("mzfile");
                String dataSheetPath = cmd.getOptionValue("sheetFile");
                String peptideFilePath = cmd.getOptionValue("hitsFile");
                String token = cmd.getOptionValue("token");
                String outputDir = cmd.getOptionValue("output");

                Quantification isoQuant = new Quantification();

                System.out.println("Reading configuration file...");
                if (quantID != null) {
                    isoQuant.setQuantId(Integer.parseInt(quantID));
                }
                isoQuant.readConfFile(configFilePath, dataSheetPath);
                isoQuant.setMzMLFilePath(mzMLFilePath);
                isoQuant.setPeptideFilePath(peptideFilePath);
                isoQuant.setToken(token);
                isoQuant.setOutputDirectory(outputDir);
                System.out.println("Quantifying...");
                System.out.println("Writing output in " + outputDir);
                isoQuant.quantify(null);
                System.out.println("Finished!");
                System.exit(0);
            }

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("IsobaricQuant", options);
            System.exit(1);
        }
    }
}
