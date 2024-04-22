package rdfpgmapper.mapper.rpt.pgtcomplete.schemamodel;

import java.util.ArrayList;
import java.util.List;

public class RDFProperty {
    private String uri;
    private Boolean literal = false;
    private List<String> domains = new ArrayList<>();
    private List<String> ranges = new ArrayList<>();
    private List<String> subproperties = new ArrayList<>();
    private List<String> superproperties = new ArrayList<>();

    public void setIsLiteral() {
        literal = true;
    }

    public Boolean isLiteral() {
        return literal;
    }

    public RDFProperty(String uri) {
        this.uri = uri;
    }

    public void addDomain(String domain) {
        domains.add(domain);
    }

    public void addRange(String range) {
        ranges.add(range);
    }

    public void addSubproperty(String subproperty) {
        subproperties.add(subproperty);
    }

    public void addSuperproperty(String superproperty) {
        superproperties.add(superproperty);
    }

    public String getUri() {
        return uri;
    }

    public List<String> getDomains() {
        return domains;
    }

    public List<String> getRanges() {
        return ranges;
    }

    public List<String> getSubproperties() {
        return subproperties;
    }

    public List<String> getSuperproperties() {
        return superproperties;
    }

}