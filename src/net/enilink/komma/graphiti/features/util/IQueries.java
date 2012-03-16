package net.enilink.komma.graphiti.features.util;

import net.enilink.komma.util.ISparqlConstants;

public interface IQueries {
	public static final String SELECT_APPLICABLE_CONNECTION_PROPERTIES = ISparqlConstants.PREFIX //
			+ "SELECT DISTINCT ?property " //
			+ "WHERE { " //
			+ "?subject a ?subjectType . " //
			+ "?object a ?objectType . " //
			+ "{" //
			+ "		?property rdfs:domain ?subjectType ." //
			+ "		?property rdfs:range ?objectType ." //
			+ "} UNION {" //
			+ "		?subjectType rdfs:subClassOf ?restriction ."
			+ "		?restriction owl:onProperty ?superProperty . ?property rdfs:subPropertyOf ?superProperty ." //
			+ "		{?restriction owl:allValuesFrom ?objectType} UNION {?restriction owl:someValuesFrom ?objectType}" //
			+ "}" //

			// exclude properties that can not be applied to
			// the actual types of the subject and the object
			// + "OPTIONAL {" //
			// + "		?property rdfs:domain ?someDomain ." //
			// + "		?property rdfs:range ?someRange ." //
			// + "		OPTIONAL {"
			// + " 		?subject a ?someDomain ."
			// + " 		?object a ?someRange ."
			// + " 		?subject a ?matchDummy ."
			// + "		}"
			// + "		FILTER (! bound(?matchDummy))"
			// + "}"
			// + "FILTER (! bound(?someDomain))"
			//
			// + "OPTIONAL {" //
			// + "		?subject rdf:type ?subjectType2 . " //
			// +
			// "		?subjectType2 rdfs:subClassOf ?restriction2 . ?restriction2 owl:onProperty ?property2; owl:allValuesFrom [owl:complementOf ?complementClass] . "
			// //
			// + "		?object rdf:type [rdfs:subClassOf ?complementClass] . "
			// + "}"
			// + "FILTER (! bound(?complementClass))"
			//
			// + "OPTIONAL {" //
			// + "	?property3 rdfs:subPropertyOf ?property ." //
			// + "	?subject rdf:type ?subjectType3 . " //
			// + " ?object rdf:type ?objectType3 . " //
			// + "	{" //
			// + "		?property3 rdfs:domain ?subjectType3 ." //
			// + "		?property3 rdfs:range ?objectType3 ." //
			// + "	} UNION {" //
			// + "		?subjectType3 rdfs:subClassOf ?restriction3 ."
			// + "		?restriction3 owl:onProperty ?property3 ." //
			// +
			// "		{?restriction3 owl:allValuesFrom ?objectType3} UNION {?restriction3 owl:someValuesFrom ?objectType3}"
			// //
			// + "	}" //
			// + "	FILTER (?property != ?property3)" //
			// + "}" //
			// + "FILTER (! bound(?property3))" //

			+ "} ORDER BY ?property";

	public static final String SELECT_APPLICABLE_CHILD_PROPERTIES = SELECT_APPLICABLE_CONNECTION_PROPERTIES;

	public static final String SELECT_APPLICABLE_CHILD_PROPERTIES_IF_OBJECT_IS_TYPE = ISparqlConstants.PREFIX //
			+ "SELECT DISTINCT ?property " //
			+ "WHERE { " //
			+ "?subject rdf:type ?subjectType . " //
			+ "?objectType rdfs:subClassOf ?superObjectType ."
			+ "{" //
			+ "		?property rdfs:domain ?subjectType ." //
			+ "		?property rdfs:range ?superObjectType ." //
			+ "} UNION {" //
			+ "		?subjectType rdfs:subClassOf ?restriction ."
			+ "		?restriction owl:onProperty ?superProperty . ?property rdfs:subPropertyOf ?superProperty ." //
			+ "		{?restriction owl:allValuesFrom ?superObjectType} UNION {?restriction owl:someValuesFrom ?superObjectType}" //
			+ "}" //
			+ "OPTIONAL {" //
			+ "		?subject rdf:type ?subjectType2 . " //
			+ "		?subjectType2 rdfs:subClassOf ?restriction2 . ?restriction2 owl:onProperty ?property2; owl:allValuesFrom [owl:complementOf ?complementClass] . " //
			+ "		?objectType rdfs:subClassOf ?complementClass . "
			+ "}"

			// exclude properties that can not be applied to
			// the actual types of the subject and the given object type
			+ "OPTIONAL {" //
			+ "		?property rdfs:domain ?someDomain ." //
			+ "		?property rdfs:range ?someRange ." //
			+ "		OPTIONAL {"
			+ " 		?subject a ?someDomain ."
			+ " 		?objectType rdfs:subClassOf ?someRange ."
			+ " 		?subject a ?matchDummy ."
			+ "		}"
			+ "		FILTER (! bound(?matchDummy))"
			+ "}"
			+ "FILTER (! bound(?someDomain) && ! bound(?complementClass))"

			+ "OPTIONAL {" //
			+ "	?property3 rdfs:subPropertyOf ?property ." //
			+ "	?subject rdf:type ?subjectType3 . " //
			+ "	{" //
			+ "		?property3 rdfs:domain ?subjectType3 ." //
			+ "		?property3 rdfs:range ?superObjectType ." //
			+ "	} UNION {" //
			+ "		?subjectType3 rdfs:subClassOf ?restriction3 ."
			+ "		?restriction3 owl:onProperty ?property3 ." //
			+ "		{?restriction3 owl:allValuesFrom ?superObjectType} UNION {?restriction3 owl:someValuesFrom ?superObjectType}" //
			+ "	}" //
			+ "	FILTER (?property != ?property3)" //
			+ "}" //
			+ "FILTER (! bound(?property3))" //
			+ "} ORDER BY ?property";

	public static final String SELECT_APPLICABLE_CONNECTION_OBJECTS = ISparqlConstants.PREFIX //
			+ "SELECT DISTINCT ?connectionType ?subjectProperty ?objectProperty " //
			+ "WHERE { " //
			+ "?subject rdf:type ?subjectType ." //
			+ "?object rdf:type ?objectType . " //
			+ "{" //
			+ "		?subjectProperty rdfs:domain ?subjectType ." //
			+ "		?subjectProperty rdfs:range ?connectionType ." //
			+ "} UNION {" //
			+ "		?subjectType rdfs:subClassOf ?subjectRestriction ."
			+ "		?subjectRestriction owl:onProperty ?subjectProperty" //
			+ "		{?subjectRestriction owl:allValuesFrom ?connectionType} UNION {?subjectRestriction owl:someValuesFrom ?connectionType}" //
			+ "}" //
			+ "?connectionType rdfs:subClassOf komma:Connection ."
			+ "{" //
			+ "		?objectProperty rdfs:domain ?connectionType ." //
			+ "		?objectProperty rdfs:range ?objectType ." //
			+ "} UNION {" //
			+ "		?connectionType rdfs:subClassOf ?objectRestriction ."
			+ "		?objectRestriction owl:onProperty ?objectProperty" //
			+ "		{?objectRestriction owl:allValuesFrom ?objectType} UNION {?objectRestriction owl:someValuesFrom ?objectType}" //
			+ "}" //

			/*
			 * + "OPTIONAL {" // + "		?subject rdf:type ?someSubjectType . " //
			 * +
			 * "		?someSubjectType rdfs:subClassOf ?otherRestr . ?otherRestr owl:onProperty ?property; owl:allValuesFrom ?someRange . "
			 * // +
			 * "		?someRange owl:complementOf ?complementClass . ?object rdf:type [rdfs:subClassOf ?complementClass] . "
			 * // +
			 * "		FILTER (?restriction != ?otherRestr && ?someSubjectType != ?otherRestr)"
			 * // + "}"
			 */

			+ "FILTER (?subjectType != ?subjectRestriction && ?objectType != ?objectRestriction)"
			// + "?property rdfs:subPropertyOf komma:relatedTo ." //
			+ "OPTIONAL {" //
			+ "	?otherSubjectProperty rdfs:subPropertyOf ?subjectProperty ." //
			+ "	?otherObjectProperty rdfs:subPropertyOf ?objectProperty ." //
			+ "	{" //
			+ "		?otherSubjectProperty rdfs:domain ?subjectType ." //
			+ "		?otherSubjectProperty rdfs:range ?connectionType ." //
			+ "	} UNION {" //
			+ "		?subjectType rdfs:subClassOf ?otherSubjectRestriction ."
			+ "		?otherSubjectRestriction owl:onProperty ?otherSubjectProperty ." //
			+ "		{?otherSubjectRestriction owl:allValuesFrom ?connectionType} UNION {?otherSubjectRestriction owl:someValuesFrom ?connectionType}"
			+ "	}" //
			+ "	{" //
			+ "		?otherObjectProperty rdfs:domain ?connectionType ." //
			+ "		?otherObjectProperty rdfs:range ?objectType ." //
			+ "	} UNION {" //
			+ "		?connectionType rdfs:subClassOf ?otherObjectRestriction ."
			+ "		?otherObjectRestriction owl:onProperty ?otherObjectProperty ." //
			+ "		{?otherObjectRestriction owl:allValuesFrom ?objectType} UNION {?otherObjectRestriction owl:someValuesFrom ?objectType}"
			+ "	}" //
			+ "	FILTER (?subjectProperty != ?otherSubjectProperty && ?objectProperty != ?otherObjectProperty)" //
			+ "}" //
			+ "FILTER (! bound(?otherSubjectProperty) && ! bound(?otherObjectProperty))" //

			+ "} ORDER BY ?connectionType";
}
