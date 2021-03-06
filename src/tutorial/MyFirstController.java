package tutorial;

// unhide if you want to convert osm to xml
import org.matsim.api.core.v01.Coord;


import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;


/* Create Facilities*/
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordImpl;


public class MyFirstController {
	
	private final static Logger log = Logger.getLogger(MyFirstController.class);
	private Scenario scenario;
	private String censusFile = "./input/census.txt";
	private String businessCensusFile = "./input/business_census.txt";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		

		// unhide if you want to convert osm to xml
		//osmToXml();
	     
		// unhide if you want to setup for VIA
		setupForVia();
		
		// unhide if you want to generate people
		//peopleGenerator();
		
		// generate facilities
/*		MyFirstController facilitiesCreator = new MyFirstController();
		facilitiesCreator.init();
		facilitiesCreator.run();
		facilitiesCreator.write();
		log.info("Creation finished #################################");*/


	}
	
	public static void setupForVia() {
		
		Config config = ConfigUtils.loadConfig("input/config.xml");
		Controler controler = new Controler(config);
		// controler.setOverwriteFiles(true);
		controler.run();
		
	}
	
	public static void osmToXml() {
		
		
	      String osm = "data/map_sg.osm";
	      Config config = ConfigUtils.createConfig();
	      Scenario sc = ScenarioUtils.createScenario(config);
	      Network net = sc.getNetwork();
	      CoordinateTransformation ct = 
	      TransformationFactory.getCoordinateTransformation(
	      TransformationFactory.WGS84, TransformationFactory.WGS84_TM);
	      OsmNetworkReader onr = new OsmNetworkReader(net,ct);
	      onr.parse(osm); 
	      new NetworkCleaner().run(net);
	      new NetworkWriter(net).write("input/network.xml");
		
	}
	
	public static void peopleGenerator() {
		
		/*
		 * We enter coordinates in the WGS84 reference system, but we want them to appear in the population file
		 * projected to UTM33N, because we also generated the network that way.
		 */
		CoordinateTransformation ct = 
			 TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_TM);
		
		/*
		 * First, create a new Config and a new Scenario.
		 */
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);

		/*
		 * Pick the Network and the Population out of the Scenario for convenience. 
		 */
		Network network = sc.getNetwork();
		Population population = sc.getPopulation();

		/*
		 * Pick the PopulationFactory out of the Population for convenience.
		 * It contains methods to create new Population items.
		 */
		PopulationFactory populationFactory = population.getFactory();

		/*
		 * Create a Person designated "1" and add it to the Population.
		 */
		Person person = populationFactory.createPerson(sc.createId("1"));
		population.addPerson(person);

		/*
		 * Create a Plan for the Person
		 */
		Plan plan = populationFactory.createPlan();
		
		/*
		 * Create a "home" Activity for the Person. In order to have the Person end its day at the same location,
		 * we keep the home coordinates for later use (see below).
		 * Note that we use the CoordinateTransformation created above.
		 */
		Coord homeCoordinates = sc.createCoord(14.31377, 51.76948);
		Activity activity1 = populationFactory.createActivityFromCoord("home", ct.transform(homeCoordinates));
		activity1.setEndTime(21600); // leave at 6 o'clock
		plan.addActivity(activity1); // add the Activity to the Plan
		
		/*
		 * Create a Leg. A Leg initially hasn't got many attributes. It just says that a car will be used.
		 */
		plan.addLeg(populationFactory.createLeg("car"));
		
		/*
		 * Create a "work" Activity, at a different location.
		 */
		Activity activity2 = populationFactory.createActivityFromCoord("work", ct.transform(sc.createCoord(14.34024, 51.75649)));
		activity2.setEndTime(57600); // leave at 4 p.m.
		plan.addActivity(activity2);
		
		/*
		 * Create another car Leg.
		 */
		plan.addLeg(populationFactory.createLeg("car"));
		
		/*
		 * End the day with another Activity at home. Note that it gets the same coordinates as the first activity.
		 */
		Activity activity3 = populationFactory.createActivityFromCoord("home", ct.transform(homeCoordinates));
		plan.addActivity(activity3);
		person.addPlan(plan);

		/*
		 * Write the population (of 1 Person) to a file.
		 */
		MatsimWriter popWriter = new org.matsim.api.core.v01.population.PopulationWriter(population, network);
		popWriter.write("./input/population.xml");
	}

	
	/* create facilities function */
	
	private void init() {
		/*
		 * Create the scenario
		 */
		Config config = ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.createScenario(config);	
	}
	
	private void run() {
		/*
		 * Read the business census for work, shop, leisure and education facilities
		 */
		int startIndex = this.readBusinessCensus();
		
		/*
		 * Read the census for home facilities. Other sources such as official dwelling directories could be used as well.
		 * Usually some aggregation should be done. In this example we simply add every home location as a facility.
		 */
		this.readCensus(startIndex);
		
	}
	
	private int readBusinessCensus() {
		int cnt = 0;
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(this.businessCensusFile));
			String line = bufferedReader.readLine(); //skip header
			
			// id = 0
			int index_xCoord = 1;
			int index_yCoord = 2;
			int index_types = 3;
			
			
			while ((line = bufferedReader.readLine()) != null) {
				String parts[] = line.split("\t");
				
				Coord coord = new CoordImpl(Double.parseDouble(parts[index_xCoord]),
						Double.parseDouble(parts[index_yCoord]));
				
				ActivityFacilityImpl facility = 
					(ActivityFacilityImpl)((ScenarioImpl)this.scenario).getActivityFacilities().createAndAddFacility(new IdImpl(cnt), coord);
				
				String types [] = parts[index_types].split(",");
 				for (int i = 0; i < types.length; i++) {
 					this.addActivityOption(facility, types[i]);
				}
				cnt++;
			}
		} // end try
		catch (IOException e) {
			e.printStackTrace();
		}
		return cnt;
	}
	
	private void readCensus(int startIndex) {
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(this.censusFile));
			String line = bufferedReader.readLine(); //skip header
			
			int index_xHomeCoord = 10;
			int index_yHomeCoord = 11;
			
			int cnt = 0;
			while ((line = bufferedReader.readLine()) != null) {
				String parts[] = line.split("\t");
				
				Coord homeCoord = new CoordImpl(Double.parseDouble(parts[index_xHomeCoord]),
						Double.parseDouble(parts[index_yHomeCoord]));
				
				ActivityFacility facility = ((ScenarioImpl)this.scenario).getActivityFacilities().createAndAddFacility(new IdImpl(startIndex + cnt), homeCoord);
				addActivityOption(facility, "home");
				cnt++;
			}
			
		} // end try
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void addActivityOption(ActivityFacility facility, String type) {
		((ActivityFacilityImpl) facility).createActivityOption(type);
		
		/*
		 * [[ 1 ]] Specify the opening hours here for shopping and leisure. An example is given for the activities work and home.
		 */
		ActivityOptionImpl actOption = (ActivityOptionImpl)facility.getActivityOptions().get(type);
		OpeningTimeImpl opentime;
		if (type.equals("shop")) {
			opentime = null;
		}
		else if (type.equals("leisure") || type.equals("education")) {
			opentime = null;
		}
		else if (type.equals("work")) {
			opentime = new OpeningTimeImpl(DayType.wkday, 8.0 * 3600.0, 19.0 * 3600); //[[ 1 ]] opentime = null;
		}
		// home
		else {
			opentime = new OpeningTimeImpl(DayType.wk, 0.0 * 3600.0, 24.0 * 3600);
		}
		actOption.addOpeningTime(opentime);	
	}
		
	public void write() {
		new FacilitiesWriter(((ScenarioImpl) this.scenario).getActivityFacilities()).write("./input/facilities.xml.gz");
	}
}
