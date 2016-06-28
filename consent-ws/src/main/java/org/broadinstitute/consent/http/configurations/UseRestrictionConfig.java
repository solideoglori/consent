package org.broadinstitute.consent.http.configurations;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class UseRestrictionConfig {

    @NotNull
    private String methods;

    @NotNull
    private String aggregate;

    @NotNull
    private String controls;

    @NotNull
    private String population;

    @NotNull
    private String male;

    @NotNull
    private String female;

    @NotNull
    private String profit;

    @NotNull
    private String nonProfit;

    @NotNull
    private String boys;

    @NotNull
    private String girls;

    @NotNull
    private String pediatric;

    public UseRestrictionConfig() {
    }


    public UseRestrictionConfig(String methods, String aggregate, String controls, String population, String male,
                                String female, String profit, String nonProfit, String boys, String girls,
                                String pediatric) {

        setMethods(methods);
        setAggregate(aggregate);
        setPopulation(population);
        setMale(male);
        setFemale(female);
        setProfit(profit);
        setNonProfit(nonProfit);
        setBoys(boys);
        setGirls(girls);
        setPediatric(pediatric);
        setControls(controls);
    }

    private Map<String, String> values = new HashMap<>();

    public String getMethods() {
        return methods;
    }

    public void setMethods(String methods) {
        this.methods = methods;
        values.put("methods", methods);
    }


    public String getControls() {
        return controls;
    }

    public void setControls(String controls) {
        this.controls = controls;
        values.put("controls", controls);
    }

    public String getAggregate() {
        return aggregate;
    }

    public void setAggregate(String aggregate) {
        this.aggregate = aggregate;
        values.put("aggregate", aggregate);
    }


    public String getPopulation() {
        return population;
    }

    public void setPopulation(String population) {
        this.population = population;
        values.put("population", population);
    }

    public String getMale() {
        return male;
    }

    public void setMale(String male) {
        this.male = male;
        values.put("male", male);
    }

    public String getFemale() {
        return female;
    }

    public void setFemale(String female) {
        this.female = female;
        values.put("female", female);
    }

    public String getProfit() {
        return profit;
    }

    public void setProfit(String profit) {
        this.profit = profit;
        values.put("profit", profit);
    }

    public String getNonProfit() {
        return nonProfit;
    }

    public void setNonProfit(String nonProfit) {
        this.nonProfit = nonProfit;
        values.put("nonProfit", profit);
    }

    public String getPediatric() {
        return pediatric;
    }

    public String getBoys() {
        return boys;
    }

    public void setBoys(String boys) {
        this.boys = boys;
        values.put("boys", boys);
    }

    public String getGirls() {
        return girls;
    }

    public void setGirls(String girls) {
        this.girls = girls;
        values.put("girls", girls);
    }

    public void setPediatric(String pediatric) {
        this.pediatric = pediatric;
        values.put("pediatric", pediatric);
    }

    public String getValueByName(String key){
        return this.values.getOrDefault(key, "Nonexistent Ontology");
    }
}