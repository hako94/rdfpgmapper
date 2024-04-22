package rdfpgmapper.mapper.rpt.pgtcomplete.schemamodel;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RDFGraphModel {
    Map<String, RDFClass> classes = new HashMap<>();
    Map<String, RDFProperty> properties = new HashMap<>();

    public void addClass(RDFClass rdfClass) {
        classes.put(rdfClass.getUri(), rdfClass);
    }

    public void addProperty(RDFProperty rdfProperty) {
        properties.put(rdfProperty.getUri(), rdfProperty);
    }

    public RDFClass getClass(String uri) {
        return classes.get(uri);
    }

    public RDFProperty getProperty(String uri) {
        return properties.get(uri);
    }


    public Collection<RDFProperty> getProperties() {
        return properties.values();
    }

    public Collection<RDFClass> getClasses() {
        return classes.values();
    }
}
