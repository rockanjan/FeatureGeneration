
import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Generates all features for the dependency
 */
public class AttributeGenerator extends
		AttributeGeneratorBase {
	Logger logger = Logger
			.getLogger(AttributeGenerator.class
					.getName());

	public AttributeGenerator(String inputFilename,
			String outputFilename) {
		super(inputFilename, outputFilename);
	}

	@Override
	public void run() {
		try {
			load();
		} catch (IOException ioe) {
			System.err.println("Error in processing input file");
			ioe.printStackTrace();
			System.exit(1);
		}
		for (Sentence s : data) {
			int verbWordIndex = s.getVerbWordIndex();
			for (DataRow dr : s) {
				//processing word level features like numeric, capitalization, periods, hyphens, colons etc
				String wordFeatures[] = getWordFeatures(dr);
				String alphaNum = wordFeatures[0]; //alpha or numeric
				String fourDigitNum = wordFeatures[1]; //Y or N
				String hasPeriods = wordFeatures[2];
				String hasHyphens = wordFeatures[3];
				String hasCapitalized = wordFeatures[4];
				String hasSlashes = wordFeatures[5]; // like a\/k\/a
				String hasColons = wordFeatures[6];
				String hasDollars = wordFeatures[7];
				String suffix1 = wordFeatures[8];
				String suffix2 = wordFeatures[9];
				String suffix3 = wordFeatures[10];
				
				//more features from fei
				String beforeAfterPredicate = getBeforeOrAfterPredicate(dr, verbWordIndex);
				String hmmBeforePredicate = getHmmBeforePredicate(s, dr, verbWordIndex);
				String hmmAfterPredicate = getHmmAfterPredicate(s, dr, verbWordIndex);
				String hmmPathToPredicate = getHmmPathToPredicate(s, dr, verbWordIndex);
				String wordPathToPredicate = getWordPath(s, dr, verbWordIndex);
				String distance = getDistance(dr, verbWordIndex);
				//for now discretize the distance, later have to use it as continuous feature
				if(! distance.equals(AttributeGeneratorBase.NO_VERB)){
					int intDistance = Integer.parseInt(distance);
					if(intDistance == 0 || intDistance == 1 || intDistance == 2){
						//do nothing
					} else if(intDistance > 2 && intDistance <= 5){
						distance = "3-5";
					} else if(intDistance >= 6 && intDistance <= 9){
						distance = "6-9";
					} else if(intDistance >=10 && intDistance <=15){
						distance = "10-15";
					} else if(intDistance >=16){
						distance = ">=16";
					}
				}
				pw.println(dr.getRowWithAppendedFeatureNew(						
						alphaNum,
						fourDigitNum,
						hasPeriods,
						hasHyphens,
						hasCapitalized,
						hasSlashes,
						hasColons,
						hasDollars,
						suffix1,
						suffix2,
						suffix3,
						beforeAfterPredicate,//start of fei's features
						hmmBeforePredicate,
						hmmAfterPredicate,
						hmmPathToPredicate,
						wordPathToPredicate,
						distance
						));
				
			}
			pw.println();
		}
		close();
	}
	
	private String[] getWordFeatures(DataRow dr){
		String word = dr.getWord();
		int wordIndex = dr.getIndex(); //to check capitalization, if it's first word or not
		String suffix1=AttributeGeneratorBase.NO_SUFFIX, suffix2=AttributeGeneratorBase.NO_SUFFIX, suffix3 = AttributeGeneratorBase.NO_SUFFIX;
		String alphaNum; //alpha or numeric
		String fourDigitNum; //Y or N
		String hasPeriods;
		String hasHyphens;
		String hasCapitalized;
		String hasSlashes; // like a\/k\/a
		String hasColons;
		String hasDollars;
		suffix1 = word.substring(word.length()-1, word.length());
		if(word.length() > 1){
			suffix2 = word.substring(word.length()-2, word.length());
		}
		if(word.length() > 2){
			suffix3 = word.substring(word.length()-3, word.length());
		}
		Pattern p = Pattern.compile("^-{0,1}[0-9]+\\.*[0-9]*"); //eg -9, 100, 100.001 etc
		Pattern p2 = Pattern.compile("^-{0,1}[0-9]*\\.*[0-9]+"); //eg. -.5, .5
		Pattern p3 = Pattern.compile("^-{0,1}[0-9]{1,3}[,[0-9]{3}]*\\.*[0-9]*"); //matches 100,000
		Pattern p4 = Pattern.compile("[0-9]+\\\\/[0-9]+"); // four \ needed, java converts it to \\
		Pattern p5 = Pattern.compile("[0-9]+:[0-9]+"); //ratios and time
		Pattern p6 = Pattern.compile("([0-9]+-)+[0-9]+"); // 1-2-3, 1-2-3-4 etc
		Matcher m = p.matcher(word);
		Matcher m2 = p2.matcher(word);
		Matcher m3 = p3.matcher(word);
		Matcher m4 = p4.matcher(word);
		Matcher m5 = p5.matcher(word);
		Matcher m6 = p6.matcher(word);
		//alpha or num
		if(m.matches() || m2.matches() || m3.matches() || m4.matches() || m5.matches() || m6.matches() ){
			//System.out.println(word);
			alphaNum = AttributeGeneratorBase.NUM;
		}
		else{
			alphaNum = AttributeGeneratorBase.ALPHA;			
		}
		//four digit
		Matcher fourDigitMatcher = Pattern.compile("[0-9]{4}").matcher(word);
		if(fourDigitMatcher.matches()){
			if(wordIndex == 0){
				
			}
			fourDigitNum = AttributeGeneratorBase.YES;
		}
		else{
			fourDigitNum = AttributeGeneratorBase.NO;
		}
		//period
		Matcher periodMatcher = Pattern.compile(".*\\..*").matcher(word);
		if(periodMatcher.matches()) hasPeriods = AttributeGeneratorBase.YES; else hasPeriods = AttributeGeneratorBase.NO;
		
		//hyphen
		Matcher hyphenMatcher = Pattern.compile(".*-.*").matcher(word);
		if(hyphenMatcher.matches()) hasHyphens = AttributeGeneratorBase.YES; else hasHyphens = AttributeGeneratorBase.NO;
		
		//capitalized
		Pattern capitalizedPattern = Pattern.compile(".*[A-Z]+.*"); 
		Matcher capitalized = capitalizedPattern.matcher(word);
		hasCapitalized = AttributeGeneratorBase.NO;
		if(capitalized.matches()){
			if(wordIndex == 0){//first word
				//remove the first char, and if it still matches, then add to hasCapitalized
				String firstCharRemoved = word.substring(1);
				Matcher cap = Pattern.compile(".*[A-Z]+.*").matcher(firstCharRemoved);
				if(cap.matches()){
					hasCapitalized = AttributeGeneratorBase.YES;
				}
			} else { //it's not first word and is capitalized
				hasCapitalized = AttributeGeneratorBase.YES;
			}
		}
		
		//has slashes (backslashes)
		Matcher slashMatcher = Pattern.compile(".*/.*").matcher(word);
		if(slashMatcher.matches()) hasSlashes = AttributeGeneratorBase.YES; else hasSlashes = AttributeGeneratorBase.NO;
		
		//colons
		Matcher colonMatcher = Pattern.compile(".*:.*").matcher(word);
		if(colonMatcher.matches()) hasColons = AttributeGeneratorBase.YES; else hasColons = AttributeGeneratorBase.NO;
		
		Matcher dollarMatcher = Pattern.compile(".*\\$.*").matcher(word);
		if(dollarMatcher.matches()) hasDollars =  AttributeGeneratorBase.YES; else hasDollars = AttributeGeneratorBase.NO;
		String wordFeatures[] = {alphaNum, fourDigitNum, hasPeriods, hasHyphens, hasCapitalized, hasSlashes, hasColons, hasDollars, suffix1, suffix2, suffix3};
		return wordFeatures;
	}
	
	//Fei feature
	private String getBeforeOrAfterPredicate(DataRow dr, int verbIndex){
		String returnValue = AttributeGeneratorBase.ERROR;
		if(verbIndex == -1){
			return AttributeGeneratorBase.NO_VERB;
		}
		if(dr.getPredicateLabel().equals("V")){
			return "V";
		}
		if(dr.getIndex() < verbIndex){
			return AttributeGeneratorBase.BEFORE;
		}
		if(dr.getIndex() > verbIndex){
			return AttributeGeneratorBase.AFTER;
		}
		return returnValue;
	}

	private String getDistance(DataRow dr, int verbIndex){
		String returnValue = AttributeGeneratorBase.ERROR;
		if(verbIndex == -1){
			return AttributeGeneratorBase.NO_VERB;
		}
		if(dr.getPredicateLabel().equals("V")){
			return "0";
		}
		returnValue = Math.abs(dr.getIndex() - verbIndex) + "";
		return returnValue;
	}
	
	private String getHmmBeforePredicate(Sentence s, DataRow dr, int verbIndex){
		String returnValue = AttributeGeneratorBase.ERROR;
		if(verbIndex == -1){
			return AttributeGeneratorBase.NO_VERB;
		}
		if(verbIndex == 0){ //if verb is itself the first word
			return AttributeGeneratorBase.OOB;
		}
		returnValue = "" + s.get(verbIndex-1).getHmmState();
		return returnValue;
	}
	
	private String getHmmAfterPredicate(Sentence s, DataRow dr, int verbIndex){
		String returnValue = AttributeGeneratorBase.ERROR;
		if(verbIndex == -1){
			return AttributeGeneratorBase.NO_VERB;
		}
		if(verbIndex == s.size()-1){
			return AttributeGeneratorBase.OOB;
		}
		returnValue = "" + s.get(verbIndex+1).getHmmState();
		return returnValue;
	}
	
	private String getHmmPathToPredicate(Sentence s, DataRow dr, int verbIndex){
		String returnValue = "";
		if(verbIndex == -1){
			return AttributeGeneratorBase.NO_VERB;
		}
		int currentIndex = dr.getIndex();
		if(currentIndex < verbIndex){
			for(int i=currentIndex+1; i < verbIndex; i++){
				returnValue += s.get(i).getHmmState();
				if(i != verbIndex-1){
					returnValue += AttributeGeneratorBase.JOIN;
				}
			}
		}
		else {
			for(int i = verbIndex + 1; i < currentIndex; i++){
				returnValue += s.get(i).getHmmState();
				if(i != currentIndex - 1){
					returnValue += AttributeGeneratorBase.JOIN;
				}
			}
		}
		if(returnValue.equals("")){
			returnValue = AttributeGeneratorBase.NO_PATH;
		}
		if(dr.getIndex() == verbIndex){
			returnValue = AttributeGeneratorBase.VERB;
		}
		return returnValue;
	}
	
	private String getWordPath(Sentence s, DataRow dr, int verbIndex){
		String returnValue = "";
		if(verbIndex == -1){
			return AttributeGeneratorBase.NO_VERB;
		}
		int currentIndex = dr.getIndex();
		if(currentIndex < verbIndex){
			for(int i=currentIndex+1; i < verbIndex; i++){
				returnValue += s.get(i).getWord();
				if(i != verbIndex-1){
					returnValue += AttributeGeneratorBase.JOIN;
				}
			}
		}
		else {
			for(int i = verbIndex + 1; i < currentIndex; i++){
				returnValue += s.get(i).getWord();
				if(i != currentIndex - 1){
					returnValue += AttributeGeneratorBase.JOIN;
				}
			}
		}
		if(returnValue.equals("")){
			returnValue = AttributeGeneratorBase.NO_PATH;
		}
		if(dr.getIndex() == verbIndex){
			returnValue = AttributeGeneratorBase.VERB;
		}
		return returnValue;
	}
		
}
