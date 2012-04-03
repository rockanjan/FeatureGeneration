public class CreateAttribute {
	public static void main(String[] args){
		if(args.length != 2){
			System.err.println("Usage: <createfeaturenew> inputfile outputfile");
			System.exit(1);
		}
		AttributeGenerator generator = new AttributeGenerator(args[0], args[1]);
		System.out.println("Running FeatureNew generator");
		generator.run();
		System.out.println("Done!");
	}
}
