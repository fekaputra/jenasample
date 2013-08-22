package at.ac.tuwien.cdl;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.util.FileManager;

/**
 * So, here is an example jena project
 * We will try use Apache Jena (tm) to load, modify, and store ontology and ontology instances 
 * 
 * In this sample I am using a simple owl (smallonto_empty.owl) for storing experiments, which contain no individual.
 * The base namespace for this ontology is: "http://www.cdl.ifs.tuwien.ac.at/emse_inspection.owl#" 
 * This owl file contain three classes: 
 * 
 * Experiment class, with 3 datatype properties: 
 * 	experimentID (functional String, identical to URI localname)
 * 	experimentDesignType (multiple String, showing a design type of an experiment)
 * 	experimentObjective (multiple String, showing objectives of an experiment)
 * 
 * Hypothesis class, with 1 object property and 2 datatype properties:
 * 	hypothesisID (functional String, identical to URI localname)
 * 	hypothesisText (multiple String, some explanation about an experiment)
 * 	hasHypothesisExperiment (multiple Experiment, showing in which Experiment this Hypothesis is used)
 * 
 * ResponseVariable class, with 2 object property and 2 datatype properties: 
 * 	responseVariableID (functional String, identical to URI localname)
 * 	responseVariableText (multiple String, some explanation about a ResponseVariable)
 * 	hasResponseVariableExperiment (multiple Experiment, showing which Experiment is related to the ResponseVariable)
 * 	hasResponseVariableHypothesis (multiple Hypothesis, showing which Hypothesis is related to the ResponseVariable)
 * 
 * @author Juang
 * @since 15.08.2013
 *
 */
public class JenaHelloWorld {
	
	/**
	 * OntModel is a representation of an ontology in Jena
	 */
	OntModel model;
	
	/**
	 * constructor
	 * 
	 * @param owlFile
	 */
	public JenaHelloWorld(String owlFile) {
		// instantiate a new empty OntModel
		model = ModelFactory.createOntologyModel();
		
		// read an owl file and tying it to the OntModel
		readOwl(owlFile);
	}
	
	/**
	 * linking OntModel with an owl file
	 * 
	 * @param owlFile
	 */
	public void readOwl(String owlFile) {
		InputStream in = FileManager.get().open(owlFile);
		if(in==null) throw new IllegalArgumentException("File: '"+owlFile+"' not found");
		model.read(in, null);
	}
	
	/**
	 * adding a new instance to a class (without adding any properties)
	 * 	alternatively we could use the insert query to do this
	 * 
	 * @param className
	 * @param instanceName 
	 */
	public void addNewInstance(String className, String instanceName) {
		OntClass ontClass = model.getOntClass(getDefaultNS()+className);
		if(ontClass==null) throw new IllegalArgumentException("Class: '"+className+"' not found");
		model.createIndividual(getDefaultNS()+instanceName, ontClass);
	}
	
	/**
	 * Since we need this a lot for accessing class, instance and so on.. I create this helper function 
	 * 	to yield the default prefix
	 * 
	 * @return default prefix for the ontology: <http://www.cdl.ifs.tuwien.ac.at/emse_inspection.owl#>
	 */
	private String getDefaultNS() {
		return model.getNsPrefixURI("");
	}
	
	/**
	 * execution of a select query
	 * 	do not forget to close QueryExecution after done iterating with the result	
	 * 
	 * @param query
	 * @return
	 */
	public void selectQuery(String query) {
		Query q = QueryFactory.create(query);
		QueryExecution qe = QueryExecutionFactory.create(q, model);
		ResultSet result = qe.execSelect();
		printResultSet(result);
		qe.close();
	}
	
	/**
	 * execution of a construct query
	 * @param query
	 */
	public void constructQuery(String query) {
		Query q = QueryFactory.create(query);
		QueryExecution qe = QueryExecutionFactory.create(q, model);
		Model result = qe.execConstruct();
		printModel(result);
		model.add(result);
		qe.close();
	}
	
	/**
	 * Using sparql update
	 * 	Alternative way to insert data into ontology
	 * 	We could also use this for modifying the ontology
	 * @param query
	 */
	public void updateQuery(String query) {
		UpdateAction.parseExecute(query, model);
	}
	
	/**
	 * print result from ResultSet
	 * 
	 * @param rs
	 */
	private void printResultSet(ResultSet rs) {
		while(rs.hasNext()) {	
			QuerySolution qs = rs.next();
			System.out.println(qs.toString());
		}
		System.out.println("------");
	}
	
	/**
	 * Print result model
	 * 	Instead of resultset, construct yield model (graph)
	 * 	So we need different way to print it
	 * 
	 * @param rs
	 */
	private void printModel(Model model) {
		Iterator<Statement> statements = model.listStatements();
		while(statements.hasNext()) {
			System.out.println(statements.next().toString());
		}
		
		System.out.println("------");
	}
	
	/**
	 * changes are not automatically saved into the ontology, 
	 * 	so you have to trigger it yourself, and choose where to store your model+changes 
	 * 
	 * @param outFile
	 */
	public void saveModel(String outFile) {
		try {
			model.write(new FileWriter(outFile), "RDF/XML");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * main program
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// instantiation
		JenaHelloWorld jena = new JenaHelloWorld("resource/smallonto_empty.owl");
		
		// sample query: find all class
		String query1 = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
						"PREFIX owl: <http://www.w3.org/2002/07/owl#>" +
						"select * " +
						"where { ?s rdf:type owl:Class }";
		jena.selectQuery(query1);
		
		// add new instance using jena structure, and then query it
		jena.addNewInstance("Experiment", "Experiment01");
		String query2 = "PREFIX : <http://www.cdl.ifs.tuwien.ac.at/emse_inspection.owl#>" +
						"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
						"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +
						"select * " +
						"where { ?s rdf:type :Experiment }";
		jena.selectQuery(query2);
		
		// add new instance using SPARQL insert, and then query it
		String query3 = "PREFIX : <http://www.cdl.ifs.tuwien.ac.at/emse_inspection.owl#>" +
						"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
						"INSERT DATA { 	:Hypothesis_01 rdf:type :Hypothesis; " +
						"				:hypothesisID \"Hypothesis_01\" }";
		jena.updateQuery(query3);
		
		// use construct query
		String query5 = "PREFIX : <http://www.cdl.ifs.tuwien.ac.at/emse_inspection.owl#>" +
						"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
						"PREFIX owl: <http://www.w3.org/2002/07/owl#>" +
						"CONSTRUCT { ?z rdf:type :Experiment }" +
						"WHERE { 	?s rdf:type :Hypothesis; " +
						"			:hypothesisID ?x " +
						"			BIND (URI(CONCAT(\"http://www.cdl.ifs.tuwien.ac.at/emse_inspection.owl#Experiment_\",?x)) AS ?z)}";
		jena.constructQuery(query5);
		
		
		jena.saveModel("resource/smallonto_filled.owl");
	}
}
