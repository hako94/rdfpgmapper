package rdfpgmapper.mapper.rpt.pgtcomplete.schemamodel;

import java.util.ArrayList;
import java.util.List;

public class RDFClass {
    private String uri;
    private List<String> subclasses = new ArrayList<>();
    private List<String> superclasses = new ArrayList<>();

    public RDFClass(String uri) {
        this.uri = uri;
    }

    public void addSubclass(String subclass) {
        subclasses.add(subclass);
    }

    public void addSuperclass(String superclass) {
        superclasses.add(superclass);
    }

    public String getUri() {
        return uri;
    }

    public List<String> getSubclasses() {
        return subclasses;
    }

    public List<String> getSuperclasses() {
        return superclasses;
    }
}