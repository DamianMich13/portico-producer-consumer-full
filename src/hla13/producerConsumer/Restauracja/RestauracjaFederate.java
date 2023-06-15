package hla13.producerConsumer.Restauracja;

import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Random;

/**
 * Created by Michal on 2016-04-27.
 */
public class RestauracjaFederate {

    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private RestauracjaAmbassador fedamb;
    private final double timeStep           = 10.0;

    private int storageHlaHandle;

    public void runFederate() throws RTIexception {
        rtiamb = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();

        try
        {
            File fom = new File( "producer-consumer.fed" );
            rtiamb.createFederationExecution( "ExampleFederation",
                    fom.toURI().toURL() );
            log( "Created Federation" );
        }
        catch( FederationExecutionAlreadyExists exists )
        {
            log( "Didn't create federation, it already existed" );
        }
        catch( MalformedURLException urle )
        {
            log( "Exception processing fom: " + urle.getMessage() );
            urle.printStackTrace();
            return;
        }

        fedamb = new RestauracjaAmbassador();
        rtiamb.joinFederationExecution( "RestauracjaFederate", "ExampleFederation", fedamb );
        log( "Joined Federation as KlienciFederate");

        rtiamb.registerFederationSynchronizationPoint( READY_TO_RUN, null );

        while( fedamb.isAnnounced == false )
        {
            rtiamb.tick();
        }

        waitForUser();

        rtiamb.synchronizationPointAchieved( READY_TO_RUN );
        log( "Achieved sync point: " +READY_TO_RUN+ ", waiting for federation..." );
        while( fedamb.isReadyToRun == false )
        {
            rtiamb.tick();
        }

        enableTimePolicy();

        publishAndSubscribe();

        while (fedamb.running) {
            double timeToAdvance = fedamb.federateTime + timeStep;
            advanceTime(timeToAdvance);

            // tutaj w petli sprawdzi dla kazdego stolika czy czas federata jest wiekszy niz czas zakonczenia pierwszego posiłku
            // jesli czas jest wiekszy sprawdź czy ma drugi posilek
            // nie ma drugiego posilku - klient sobie idzie
            // ma drugi posilek - sprawdz czy czas federata jest wiekszy niz czas drugiego posilku
            // jesli jest to klient sobie idzie
            // jesli nie to sie nic nie dzieje
            // fedamb.federateTime - czas federata

            // Jezeli sa zajete stoliki
            if (fedamb.tablesArray.size()>0){
                // Przelec przez wszystkie stoliki
                for (Table table : fedamb.tablesArray){
                    // Jezeli czas federata wiekszy od czasu zjedzenie
                    if (fedamb.federateTime>table.getMealTime()){
                        // Jezeli ma drugi posilek
                        if (table.getSecondMealTime()>0){
                            // Jezeli czas federata jest wiekszy niz czas posilku to sobie idzie
                            if (fedamb.federateTime> table.getSecondMealTime()){
                                fedamb.tablesArray.remove(table);
                                fedamb.tables++;
                            }
                            // Jak czas nie jest wiekszy to nic sie nie dzieje

                        }
                        // Jak nie ma drugiego posilku to sobie idzie
                        else {
                            fedamb.tablesArray.remove(table);
                            fedamb.tables++;
                        }
                    }
                }
            }



            //sendInteraction(fedamb.federateTime + fedamb.federateLookahead);
            /*if(fedamb.grantedTime == timeToAdvance) {
                timeToAdvance += fedamb.federateLookahead;
                log("Updating stock at time: " + timeToAdvance);
                updateHLAObject(timeToAdvance);
                fedamb.federateTime = timeToAdvance;
            }*/
            rtiamb.tick();
        }

    }



    private void waitForUser()
    {
        log( " >>>>>>>>>> Press Enter to Continue <<<<<<<<<<" );
        BufferedReader reader = new BufferedReader( new InputStreamReader(System.in) );
        try
        {
            reader.readLine();
        }
        catch( Exception e )
        {
            log( "Error while waiting for user input: " + e.getMessage() );
            e.printStackTrace();
        }
    }

    /*public void addToStock(int qty) {
        this.tables += qty;
        log("Added "+ qty + " at time: "+ fedamb.federateTime +", current stock: " + this.tables);
    }

    public void getFromStock(int qty) {

        if(this.tables - qty < 0) {
            log("Not enough product at stock");
        }
        else {
            this.tables -= qty;
            log("Removed "+ qty + " at time: "+ fedamb.federateTime +", current stock: " + this.tables);
        }


    }*/
    private void enableTimePolicy() throws RTIexception
    {
        LogicalTime currentTime = convertTime( fedamb.federateTime );
        LogicalTimeInterval lookahead = convertInterval( fedamb.federateLookahead );

        this.rtiamb.enableTimeRegulation( currentTime, lookahead );

        while( fedamb.isRegulating == false )
        {
            rtiamb.tick();
        }

        this.rtiamb.enableTimeConstrained();

        while( fedamb.isConstrained == false )
        {
            rtiamb.tick();
        }
    }

    private void sendInteraction(double timeStep) throws RTIexception {
        SuppliedParameters parameters =
                RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
        Random random = new Random();
        int quantityInt = random.nextInt(10) + 1;
        byte[] quantity = EncodingHelpers.encodeInt(quantityInt);

        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.ZajecieStolika");
        int quantityHandle = rtiamb.getParameterHandle( "id", interactionHandle );

        parameters.add(quantityHandle, quantity);

        LogicalTime time = convertTime( timeStep );
        log("Sending ZajecieStolika: " + quantityInt);
        // TSO
        rtiamb.sendInteraction( interactionHandle, parameters, "tag".getBytes(), time );
//        // RO
//        rtiamb.sendInteraction( interactionHandle, parameters, "tag".getBytes() );
    }

    private void publishAndSubscribe() throws RTIexception {
        int zajecieStolikaHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.ZajecieStolika" );
        fedamb.zajecieStolikaHandle = zajecieStolikaHandle;
        rtiamb.subscribeInteractionClass(zajecieStolikaHandle);
        int classHandle = rtiamb.getObjectClassHandle("ObjectRoot.Storage");
        int stockHandle    = rtiamb.getAttributeHandle( "stock", classHandle );

        AttributeHandleSet attributes =
                RtiFactoryFactory.getRtiFactory().createAttributeHandleSet();
        attributes.add( stockHandle );
        rtiamb.subscribeObjectClassAttributes(classHandle, attributes);

        int classHandle1 = rtiamb.getObjectClassHandle("ObjectRoot.Table");
        int stockHandle1    = rtiamb.getAttributeHandle( "stock", classHandle1 );

        AttributeHandleSet attributes1 =
                RtiFactoryFactory.getRtiFactory().createAttributeHandleSet();
        attributes1.add( stockHandle1 );
        rtiamb.publishObjectClass(classHandle1, attributes1);
    }

    public void updateHLAObject(double time) throws RTIexception{
        SuppliedAttributes attributes =
                RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();

        int classHandle = rtiamb.getObjectClass(storageHlaHandle);
        int stockHandle = rtiamb.getAttributeHandle( "stock", classHandle );
        byte[] stockValue = EncodingHelpers.encodeInt(fedamb.tables);

        attributes.add(stockHandle, stockValue);
        LogicalTime logicalTime = convertTime( time );
        rtiamb.updateAttributeValues( storageHlaHandle, attributes, "actualize stock".getBytes(), logicalTime );
    }

    private void advanceTime( double timestep ) throws RTIexception
    {
        log("requesting time advance for: " + timestep);
        // request the advance
        fedamb.isAdvancing = true;
        LogicalTime newTime = convertTime( fedamb.federateTime + timestep );
        rtiamb.timeAdvanceRequest( newTime );
        while( fedamb.isAdvancing )
        {
            rtiamb.tick();
        }
    }

    private double randomTime() {
        Random r = new Random();
        return 1 +(4 * r.nextDouble());
    }

    private LogicalTime convertTime( double time )
    {
        // PORTICO SPECIFIC!!
        return new DoubleTime( time );
    }

    /**
     * Same as for {@link #convertTime(double)}
     */
    private LogicalTimeInterval convertInterval( double time )
    {
        // PORTICO SPECIFIC!!
        return new DoubleTimeInterval( time );
    }

    private void log( String message )
    {
        System.out.println( "RestauracjaFederate   : " + message );
    }

    public static void main(String[] args) {
        try {
            new RestauracjaFederate().
                    runFederate();
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }
    }


}
