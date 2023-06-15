package hla13.producerConsumer.Restauracja;

import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.NullFederateAmbassador;
import hla13.Example13Federate;
import org.portico.impl.hla13.types.DoubleTime;
import java.util.Random;
import java.util.ArrayList;

/**
 * Created by Michal on 2016-04-27.
 */
public class RestauracjaAmbassador extends NullFederateAmbassador {

    protected double federateTime = 0.0;
    protected double federateLookahead = 1.0;
    protected double grantedTime = 0.0;
    protected boolean isRegulating = false;
    protected boolean isConstrained = false;
    protected boolean isAdvancing = false;

    protected boolean isAnnounced = false;
    protected boolean isReadyToRun = false;

    protected boolean running = true;

    protected Random rngGenerator = new Random();
    protected int zajecieStolikaHandle = 0;

    protected int tables = 10;

    protected ArrayList<Table> tablesArray = new ArrayList<>();
    protected RestauracjaFederate restauracjaFederate = new RestauracjaFederate();

    private double convertTime(LogicalTime logicalTime) {
        // PORTICO SPECIFIC!!
        return ((DoubleTime) logicalTime).getTime();
    }

    private void log(String message) {
        System.out.println("FederateAmbassador: " + message);
    }

    public void synchronizationPointRegistrationFailed(String label) {
        log("Failed to register sync point: " + label);
    }

    public void synchronizationPointRegistrationSucceeded(String label) {
        log("Successfully registered sync point: " + label);
    }

    public void announceSynchronizationPoint(String label, byte[] tag) {
        log("Synchronization point announced: " + label);
        if (label.equals(Example13Federate.READY_TO_RUN))
            this.isAnnounced = true;
    }

    public void federationSynchronized(String label) {
        log("Federation Synchronized: " + label);
        if (label.equals(Example13Federate.READY_TO_RUN))
            this.isReadyToRun = true;
    }

    /**
     * The RTI has informed us that time regulation is now enabled.
     */
    public void timeRegulationEnabled(LogicalTime theFederateTime) {
        this.federateTime = convertTime(theFederateTime);
        this.isRegulating = true;
    }

    public void timeConstrainedEnabled(LogicalTime theFederateTime) {
        this.federateTime = convertTime(theFederateTime);
        this.isConstrained = true;
    }

    public void timeAdvanceGrant(LogicalTime theTime) {
        this.federateTime = convertTime(theTime);
        this.isAdvancing = false;
    }

    public void receiveInteraction(int interactionClass,
                                   ReceivedInteraction theInteraction,
                                   byte[] tag) {
        // just pass it on to the other method for printing purposes
        // passing null as the time will let the other method know it
        // it from us, not from the RTI
        receiveInteraction(interactionClass, theInteraction, tag, null, null);
    }

    public void receiveInteraction(int interactionClass,
                                   ReceivedInteraction theInteraction,
                                   byte[] tag,
                                   LogicalTime theTime,
                                   EventRetractionHandle eventRetractionHandle) {
        StringBuilder builder = new StringBuilder("Interaction Received:");
        if (interactionClass == zajecieStolikaHandle) {
            try {
                int id = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                double time = convertTime(theTime);

                builder.append("Zajecie Stolika , time=" + time);
                builder.append(" id=").append(id);
                builder.append("\n");

                // Zmniejsz liczbe stolikow
                tables--;

                // Wylosuj czasy
                int gornaGranicaPierwszyPosilek=20;
                int dolnaGranicaPierwszyPosilek=5;

                int gornaGranicaDrugiPosilek=30;
                int dolnaGranicaDrugiPosilek=15;


                double mealTime = rngGenerator.nextInt(gornaGranicaPierwszyPosilek-dolnaGranicaPierwszyPosilek+1)+dolnaGranicaPierwszyPosilek;

                // Losuje czy chce drugi posilek
                if (rngGenerator.nextBoolean()){
                    double secondMealTime = rngGenerator.nextInt(gornaGranicaDrugiPosilek-dolnaGranicaDrugiPosilek+1)+dolnaGranicaDrugiPosilek;
                    Table table = new Table(time+mealTime,time+mealTime+secondMealTime);
                    log("Dodano stolik, czas pierwszego posilku: " + table.getMealTime() + ", czas drugiego posilku: " + table.getSecondMealTime());
                    this.tablesArray.add(table);
                } else {
                    Table table = new Table(time+mealTime,0.0);
                    this.tablesArray.add(table);
                    log("Dodano stolik, czas pierwszego posilku: " + table.getMealTime());
                }



            } catch (ArrayIndexOutOfBounds ignored) {

            }

            log(builder.toString());
        }
    }

    private void eating(){

    }

    public void reflectAttributeValues(int theObject,
                                       ReflectedAttributes theAttributes, byte[] tag) {
        reflectAttributeValues(theObject, theAttributes, tag, null, null);
    }

    public void reflectAttributeValues(int theObject,
                                       ReflectedAttributes theAttributes, byte[] tag, LogicalTime theTime,
                                       EventRetractionHandle retractionHandle) {
        StringBuilder builder = new StringBuilder("Reflection for object:");

        builder.append(" handle=").append(theObject);
//		builder.append(", tag=" + EncodingHelpers.decodeString(tag));

        // print the attribute information
        builder.append(", attributeCount=").append(theAttributes.size());
        builder.append("\n");
        for (int i = 0; i < theAttributes.size(); i++) {
            try {
                // print the attibute handle
                builder.append("\tattributeHandle=");
                builder.append(theAttributes.getAttributeHandle(i));
                // print the attribute value
                builder.append(", attributeValue=");
                builder.append(EncodingHelpers.decodeInt(theAttributes
                        .getValue(i)));
                builder.append(", time=");
                builder.append(theTime);
                builder.append("\n");
            } catch (ArrayIndexOutOfBounds aioob) {
                // won't happen
            }
        }

        log(builder.toString());
    }
}
