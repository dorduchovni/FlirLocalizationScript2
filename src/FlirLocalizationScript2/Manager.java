package FlirLocalizationScript2;


import org.apache.commons.collections.ArrayStack;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.nio.file.Paths.get;

/**
 * Created by Dor on 8/23/15.
 */
public class Manager {


    private  ArrayList<String> keysList = new ArrayList<>();
    private String directory;

    private void iosTranslation(LinkedHashMap<String, String> finalMapAfterTranslation, LinkedHashMap<String, String> translatedMap, LinkedHashMap<String, String> englishStringsMap, String timestamp, String language, String sheetname) throws IOException {

        LinkedHashMap<String, String> untranslatedStringsMap = untranslatedCheck(translatedMap, englishStringsMap);
        if (untranslatedStringsMap.size() > 0) {
            writeToExcelFile(untranslatedStringsMap,  timestamp, sheetname,language+"_UNTRANSLATED_STIRNGS");
        }
        LinkedHashMap<String, String> additionalStringsMap = additionalCheck(translatedMap, englishStringsMap);
        if (additionalStringsMap.size() > 0) {
            writeToExcelFile(additionalStringsMap, timestamp, sheetname,language+ "_ADDITIONAL_STRINGS");
        }

        LinkedHashMap<String,String> valuesThatWasReplacedWithKeys = replacedTranslationWithKeyCheck(translatedMap);
        if (valuesThatWasReplacedWithKeys.size()>0) {
            writeToExcelFile(valuesThatWasReplacedWithKeys,timestamp,sheetname,language+"_VALUES_REPLACED_WITH_KEYS");
        }


        writeToIosStringFile(finalMapAfterTranslation, timestamp,sheetname,language);


    }
/**
    public static boolean[] sourceToExcel(String strings, String output, String platform) throws IOException {
        LinkedHashMap<String, String> stringsMap;
        String cleanStrings = null;
        try {
            cleanStrings = cleanStrings(strings, platform);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        File stringsFile = new File(cleanStrings);
        stringsMap = platformSourceFileToMap(stringsFile, "\t");
        boolean[] results = new boolean[3];
        results[0] = lpCheck(stringsMap);
        results[1] = quotationMarksCheck(stringsMap);
        results[2] = false;

        if (platform.equals("Android")) {
            results[2] = apostropheCheck(stringsMap);
        }

        writeToExcelFile(stringsMap, output);

        return results;
    }
 */

    public LinkedHashMap platformSourceFileToMap(File file, String letter) throws IOException {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = br.readLine();
        while (line != null) {
            if (line.length() != 0) {
                int index = line.indexOf(letter);
                if (line.indexOf(letter) == line.length() - 1) {
                    map.put(line.substring(0, index), "");
                } else {
                    try {
                        map.put(line.substring(0, index), line.substring(index + 1, line.length()));
                    } catch (StringIndexOutOfBoundsException e) {
                        e.printStackTrace();
                        System.out.print(line);
                    }
                }
            }
            line = br.readLine();
        }


        for (String key : map.keySet()) {
            String value = map.get(key);
            if (value.startsWith("\"")) {
                map.put(key, value.substring(1, value.length()));
            }
            value = map.get(key);
            if (value.endsWith("\"")) {
                map.put(key, value.substring(0, value.length() - 1));
            }
        }
        return map;
    }

    public void writeToIosStringFile(Map<String, String> map, String timestamp, String sheetname, String language) throws IOException {

        String fs = System.getProperty("file.separator");
        String path = directory+fs+timestamp+fs+sheetname+fs+language+fs;
        File dir = new File (path);
        dir.mkdirs();
        FileWriter fw = new FileWriter(new File(dir, "StringsForUI.strings"));

        fw.write("/*\n" +
                " StringsForUI.strings\n" +
                " FlirFX\n" +
                " \n" +
                " Created by Maor Atlas on 6/17/14.\n" +
                " Copyright (c) 2014 Zemingo. All rights reserved.\n" +
                " */" + "\n");
        for (String key : map.keySet()) {
            fw.write("\"" + key + "\"" + " = " + "\"" + map.get(key) + "\"" + ";" + "\n");
        }
        fw.close();

    }

    public void androidTranslation(String file, LinkedHashMap<String, String> translatedStringsMap, String timestamp, String language, String sheetName) throws IOException, SAXException, ParserConfigurationException, TransformerException {
        String fs = System.getProperty("file.separator");
        String path = directory+fs+timestamp+fs+sheetName+fs+language+fs;
        File dir = new File (path);
        dir.mkdirs();
        LinkedHashMap<String, String> englishStringsMap = new LinkedHashMap<>();


        File xmlToParse = new File(file);
        DocumentBuilderFactory dbFactory
                = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlToParse);
        doc.setXmlStandalone(true);
        doc.getDocumentElement().normalize();

        NodeList stringNode = doc.getElementsByTagName("string");
        for (int i = 0; i < stringNode.getLength(); i++) {
            String key = stringNode.item(i).getAttributes().getNamedItem("name").getNodeValue();
            String newValue = translatedStringsMap.get(key);
            if (newValue != null && !newValue.equals("")) {
                stringNode.item(i).setTextContent(StringEscapeUtils.unescapeXml(newValue)); //TODO test it!
            }
            englishStringsMap.put(key, stringNode.item(i).getTextContent());
        }

        stringNode = doc.getElementsByTagName("item");
        for (int i = 0; i < stringNode.getLength(); i++) {
            if (stringNode.item(i).getParentNode().getNodeName().equals("plurals")) {
                String key = stringNode.item(i).getParentNode().getAttributes().getNamedItem("name").getNodeValue() + "-" + stringNode.item(i).getAttributes().getNamedItem("quantity").getNodeValue();
                String newValue = translatedStringsMap.get(key);
                if (newValue != null) {
                    stringNode.item(i).setTextContent(StringEscapeUtils.unescapeXml(newValue));
                }
                englishStringsMap.put(key, stringNode.item(i).getTextContent());
            }

            if (stringNode.item(i).getParentNode().getNodeName().equals("string-array")) {
                String key = stringNode.item(i).getParentNode().getAttributes().getNamedItem("name").getNodeValue() + "-" + stringNode.item(i).getTextContent();
                String newValue = translatedStringsMap.get(key);
                if (newValue != null) {
                    stringNode.item(i).setTextContent(StringEscapeUtils.unescapeXml(newValue));
                }
                englishStringsMap.put(key, stringNode.item(i).getTextContent());
            }

        }

        LinkedHashMap<String, String> untranslatedStringsMap = untranslatedCheck(translatedStringsMap, englishStringsMap);
        if (untranslatedStringsMap.size() > 0) {
            writeToExcelFile(untranslatedStringsMap, timestamp, sheetName,language+"_UNTRANSLATED_STRINGS");
        }
        LinkedHashMap<String, String> additionalStringsMap = additionalCheck(translatedStringsMap, englishStringsMap);
        if (additionalStringsMap.size() > 0) {
            writeToExcelFile(additionalStringsMap, timestamp, sheetName,language+"_ADDITIONAL_STRINGS");
        }
        LinkedHashMap<String,String> valuesThatWasReplacedWithKeys = replacedTranslationWithKeyCheck(translatedStringsMap);
        if (valuesThatWasReplacedWithKeys.size()>0) {
            writeToExcelFile(valuesThatWasReplacedWithKeys,timestamp,sheetName,language+"_VALUES_REPLACED_WITH_KEYS");
        }


        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        StreamResult result = new StreamResult(new File(dir, "strings.xml"));
        transformer.transform(source, result);


    }

    private  LinkedHashMap<String, String> additionalCheck(LinkedHashMap<String, String> translatedStringsMap, Map<String, String> sourceMap) {
        LinkedHashMap<String, String> additionalMap = new LinkedHashMap<>();
        for (String translatedKey : translatedStringsMap.keySet()) {
            if (!translatedKey.equals("")) {
                if (sourceMap.get(translatedKey) == null) {
                    additionalMap.put(translatedKey, translatedStringsMap.get(translatedKey));
                }
            }
        }
        return additionalMap;

    }

    private  LinkedHashMap<String, String> replacedTranslationWithKeyCheck(LinkedHashMap<String, String> translatedStringsMap) {
        LinkedHashMap<String, String> replacedTranslations = new LinkedHashMap<>();
        for (String key : translatedStringsMap.keySet()) {
            if (keysList.contains(translatedStringsMap.get(key))) {

                replacedTranslations.put(key,translatedStringsMap.get(key));
            }
        }
        return replacedTranslations;

    }

    private LinkedHashMap<String, String> untranslatedCheck(LinkedHashMap<String, String> translatedStringsMap, Map<String, String> sourceMap) {
        LinkedHashMap<String, String> untranslatedMap = new LinkedHashMap<>();
        for (String engKey : sourceMap.keySet()) {
            if (!engKey.equals("")) {
                if ((translatedStringsMap.get(engKey) == null) || (translatedStringsMap.get(engKey).equals("") && !sourceMap.get(engKey).equals(""))) {
                    untranslatedMap.put(engKey, sourceMap.get(engKey));
                }
            }
        }
        return untranslatedMap;

    }

    public void writeToExcelFile(Map<String, String> map, String timestamp, String sheetName, String language) throws IOException {
        String fs = System.getProperty("file.separator");
        String path = directory+fs+timestamp+fs+sheetName+fs+language+fs;
        File dir = new File (path);
        dir.mkdirs();

        FileWriter fw = new FileWriter(new File(dir, "Strings table.txt"));


        for (String key : map.keySet()) {
            fw.write(key + "\t" + map.get(key) + "\n");
        }
        fw.close();

    }

    public LinkedHashMap translate(Map<String, String> translatedStringsMap, Map<String, String> englishStringsMap) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>(englishStringsMap);
        for (String key : translatedStringsMap.keySet()) {
            if (result.containsKey(key)) {
                if (translatedStringsMap.get(key).equals("")) {
                    result.put(key, englishStringsMap.get(key));
                } else {
                    result.put(key, translatedStringsMap.get(key));
                }
            }
        }
        return result;
    }

    public void lpCheck(Map<String, String> map) {
        String[] lpToCheck = new String[]{"LP：", "LP: ", "LP:"};
        for (String lp : lpToCheck) {
            for (String key : map.keySet()) {
                String value = map.get(key);
                if (value.contains(lp)) {
                    String newValue = value.substring(0, value.indexOf(lp)) + value.substring(lp.length(), value.length());
                    map.put(key, newValue);
                }
            }
        }
    }

    public void quotationMarksCheck(Map<String, String> map) {
        for (String key : map.keySet()) {
            String value = map.get(key);
            String[] valueArr = value.split("\"");
            String newValue = "";
            if (valueArr.length > 1) {
                for (int j = 0; j < valueArr.length - 1; j++) {
                    newValue = newValue + valueArr[j];
                    if (!valueArr[j].endsWith("\\")) {
                        newValue = newValue + "\\" + "\"";
                    } else {
                        newValue = newValue + "\"";
                    }
                }
                if (valueArr[valueArr.length - 1].endsWith("\\")) {
                    newValue = newValue + valueArr[valueArr.length - 1] + "\"";
                } else {
                    newValue = newValue + valueArr[valueArr.length - 1];
                }
                map.put(key, newValue);
            }
        }
    }

    public void apostropheCheck(Map<String, String> map) {
        for (String key : map.keySet()) {
            String value = map.get(key);
            String[] valueArr = value.split("\'");
            String newValue = "";
            if (valueArr.length > 1) {
                for (int j = 0; j < valueArr.length - 1; j++) {
                    newValue = newValue + valueArr[j];
                    if (!valueArr[j].endsWith("\\")) {
                        newValue = newValue + "\\" + "\'";
                    } else {
                        newValue = newValue + "\'";
                    }
                }
                if (valueArr[valueArr.length - 1].endsWith("\\")) {
                    newValue = newValue + valueArr[valueArr.length - 1] + "\'";
                } else {
                    newValue = newValue + valueArr[valueArr.length - 1];
                }
                map.put(key, newValue);
            }

        }
    }

    public String cleanStrings(String file, String platform) throws IOException, ParserConfigurationException, SAXException {
        if (platform.equals("iOS")) {
            FileWriter fw = new FileWriter("ios.tmp");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = br.readLine();
            while (line != null) {
                if (line.indexOf("\"") == 0) {
                    String newLine = line.substring(1, line.length() - 2);
                    if (newLine.indexOf("\" = \"") != -1) {
                        newLine = newLine.replaceAll("\" = \"", "\t");
                    }

                    if (newLine.indexOf("\"=\"") != -1) {
                        newLine = newLine.replaceAll("\"=\"", "\t");
                    }

                    fw.write(newLine + "\n");

                }
                line = br.readLine();
            }
            fw.close();
            br.close();
            return ("ios.tmp");
        } else if (platform.equals("Android")) {

            FileWriter fw = new FileWriter("Android.tmp");

            File xmlToParse = new File(file);
            DocumentBuilderFactory dbFactory
                    = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlToParse);
            doc.getDocumentElement().normalize();

            NodeList stringNode = doc.getElementsByTagName("string");
            for (int i = 0; i < stringNode.getLength(); i++) {
                String key = stringNode.item(i).getAttributes().getNamedItem("name").getNodeValue();

                String value = stringNode.item(i).getTextContent();
                fw.write(key + "\t" + value + "\n");
            }


            stringNode = doc.getElementsByTagName("item");
            for (int i = 0; i < stringNode.getLength(); i++) {
                if (stringNode.item(i).getParentNode().getNodeName().equals("plurals")) {
                    String key = stringNode.item(i).getParentNode().getAttributes().getNamedItem("name").getNodeValue() + "-" + stringNode.item(i).getAttributes().getNamedItem("quantity").getNodeValue();
                    String value = stringNode.item(i).getTextContent();
                    fw.write(key + "\t" + value + "\n");
                }
                if (stringNode.item(i).getParentNode().getNodeName().equals("string-array")) {
                    String key = stringNode.item(i).getParentNode().getAttributes().getNamedItem("name").getNodeValue() + "-" + stringNode.item(i).getTextContent();
                    String value = stringNode.item(i).getTextContent();
                    fw.write(key + "\t" + value + "\n");
                }
            }

            fw.close();
            return ("Android.tmp");
        }
        return null;

    }

    public LinkedHashMap<String, List<String>> getSheetsAndLanguages(String filename) throws IOException {
        LinkedHashMap<String, List<String>> sheetsAndLanguagesMap = new LinkedHashMap<>();
        FileInputStream file = new FileInputStream(new File(filename));
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            XSSFSheet sheet = workbook.getSheetAt(i);
            List<String> languagesList = new ArrayList<>();
            Iterator<Cell> firstRowIterator = sheet.getRow(0).cellIterator();
            while (firstRowIterator.hasNext()) {
                Cell cell = firstRowIterator.next();
                if (!cell.isPartOfArrayFormulaGroup() && (cell.getStringCellValue().contains("@") || cell.getStringCellValue().contains("key"))) {
                    String language = cell.getStringCellValue().replaceAll("@", "");
                    languagesList.add(language);
                }
            }
            sheetsAndLanguagesMap.put(sheet.getSheetName(), languagesList);
        }



        Row row;
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            int keyIndex=0;
            XSSFSheet sheet = workbook.getSheetAt(i);
            Iterator<Row> rowIterator = sheet.rowIterator();
            Iterator<Cell> firstRowIterator = sheet.getRow(0).cellIterator();

            //searching for key index
            while (firstRowIterator.hasNext()) {
                Cell cell = firstRowIterator.next();
                if (!cell.isPartOfArrayFormulaGroup() && cell.getStringCellValue().equals("Key")) {
                    keyIndex = cell.getColumnIndex();
                    break;
                }
            }
            if (rowIterator.hasNext()) {
                row = rowIterator.next();
            }
            while (rowIterator.hasNext()) {
                row = rowIterator.next();
                //For each row, iterate through all the columns
                Iterator<Cell> cellIterator = row.cellIterator();
                if (row.getCell(keyIndex) != null) {
                    String key = row.getCell(keyIndex).getStringCellValue();
                    if (!key.equals("")) {
                        keysList.add(key);
                    }
                }
            }
        }





        return sheetsAndLanguagesMap;
    }

    public void translateSheetsMap(Map<String, SheetToTranslate> sheetToTranslateMap, String excelFile,String selectedDirectory) throws IOException {
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss").format(Calendar.getInstance().getTime());
        directory = selectedDirectory;
        for (String sheet : sheetToTranslateMap.keySet()) {
            SheetToTranslate sheetToTranslate = sheetToTranslateMap.get(sheet);
            LinkedHashMap<String, LinkedHashMap<String, String>> mapOfTranslationsForSheet = sheetToMaps(excelFile, sheet, sheetToTranslate.languages);
            translateEachLanguage(mapOfTranslationsForSheet, sheetToTranslate.sourceFileName, sheetToTranslate.platform, timeStamp, sheet);
        }

    }

    private void translateEachLanguage(LinkedHashMap<String, LinkedHashMap<String, String>> mapOfTranslationsForSheet, String sourceFileName, String platform, String timeStamp, String sheetname) throws IOException {

        for (String language : mapOfTranslationsForSheet.keySet()) {
            LinkedHashMap<String, String> translatedStringsMap = mapOfTranslationsForSheet.get(language);
            LinkedHashMap<String, String> englishStringsMap;
            String cleanStrings = null;
            try {
                cleanStrings = cleanStrings(sourceFileName, platform);
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
            File stringsFile = new File(cleanStrings);
            englishStringsMap = platformSourceFileToMap(stringsFile, "\t");
            lpCheck(translatedStringsMap);
            quotationMarksCheck(translatedStringsMap);

            if (platform.equals("Android")) {
                apostropheCheck(translatedStringsMap);
                try {
                    androidTranslation(sourceFileName, translatedStringsMap, timeStamp,language,sheetname);
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (TransformerException e) {
                    e.printStackTrace();
                }

            } else if (platform.equals("iOS")) {
                LinkedHashMap<String, String> finalMapAfterTranslation = translate(translatedStringsMap, englishStringsMap);
                iosTranslation(finalMapAfterTranslation, translatedStringsMap, englishStringsMap, timeStamp,language,sheetname);
            }

            Path path = get(platform + ".tmp");
            Files.delete(path);

          //  return results;


        }
    }

    public LinkedHashMap<String, LinkedHashMap<String, String>> sheetToMaps(String filename, String sheetName, List<String> languagesList) throws IOException {

        FileInputStream file = new FileInputStream(new File(filename));

        //Create Workbook instance holding reference to .xlsx file
        XSSFWorkbook workbook = new XSSFWorkbook(file);

        //Get first/desired sheet from the workbook
        XSSFSheet sheet = workbook.getSheet(sheetName);
        LinkedHashMap<String, Integer> languagesIndex = new LinkedHashMap<>();
        Iterator<Row> rowIterator = sheet.rowIterator();
        Row row = rowIterator.next();
        Iterator<Cell> firstRowIterator = sheet.getRow(0).cellIterator(); // TODO: Change to row.cellIterator()
        while (firstRowIterator.hasNext()) {
            Cell cell = firstRowIterator.next();
            if (!cell.isPartOfArrayFormulaGroup()) {
                String cellValue = cell.getStringCellValue();

                if ((cellValue.contains("@") && languagesList.contains(cellValue.replaceAll("@", "")))|| (cellValue.contains("Key"))){
                    cellValue = cellValue.replaceAll("@", "");
                    String language = cellValue;
                    languagesIndex.put(language, cell.getColumnIndex());
                }
            }
        }

        LinkedHashMap<String, LinkedHashMap<String, String>> translationsMap = new LinkedHashMap<>();
        for (String key : languagesIndex.keySet()) {
            if (!key.equals("Key")) {
                translationsMap.put(key, new LinkedHashMap<String, String>());
            }
        }
        while (rowIterator.hasNext()) {
            row = rowIterator.next();
            //For each row, iterate through all the columns
            Iterator<Cell> cellIterator = row.cellIterator();
            if (row.getCell(languagesIndex.get("Key"))!= null) {
                String key = row.getCell(languagesIndex.get("Key")).getStringCellValue();
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    if (!sheet.getRow(0).getCell(cell.getColumnIndex()).isPartOfArrayFormulaGroup()) {
                        String language = sheet.getRow(0).getCell(cell.getColumnIndex()).getStringCellValue().replaceAll("@", "");
                        if (languagesIndex.containsKey(language) && !language.equals("Key")) {
                            String value = row.getCell(languagesIndex.get(language)).getStringCellValue();
                            translationsMap.get(language).put(key, value);
                        }

                    }
                }
            }
        }
        return translationsMap;
    }
}