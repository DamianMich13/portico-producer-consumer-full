package hla13.producerConsumer.Kolejka;


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

public class KolejkaFederate {

    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private KolejkaAmbassador fedamb;
    private final double timeStep           = 1.0;

    public int tableStock = 6;
    private int storageHlaHandle;

    private int i = 1;

    public void runFederate() throws Exception {

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

        fedamb = new KolejkaAmbassador();
        rtiamb.joinFederationExecution( "KolejkaFederate", "ExampleFederation", fedamb );
        log( "Joined Federation as KolejkaFederate");

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

        registerStorageObject();

        while (fedamb.running) {
            double timeToAdvance = fedamb.federateTime + timeStep;
            advanceTime(timeToAdvance);
            fedamb.federateTime = timeToAdvance; // aby zaktualizować czas federata
            //sendInteraction(timeToAdvance + fedamb.federateLookahead);
            //timeToAdvance += fedamb.federateLookahead;
            while(tableStock >0 && !fedamb.queue.isEmpty() && fedamb.grantedTime == timeToAdvance){
                timeToAdvance += fedamb.federateLookahead;
                sendInteraction(timeToAdvance);
                //fedamb.federateTime = timeToAdvance;
            }
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

    private void registerStorageObject() throws RTIexception {
        int classHandle = rtiamb.getObjectClassHandle("ObjectRoot.Storage");
        this.storageHlaHandle = rtiamb.registerObjectInstance(classHandle);
    }

    private void updateHLAObject(double time) throws RTIexception{
        SuppliedAttributes attributes =
                RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();

        int classHandle = rtiamb.getObjectClass(storageHlaHandle);
        int stockHandle = rtiamb.getAttributeHandle( "stock", classHandle );
        byte[] stockValue = EncodingHelpers.encodeInt(i);

        attributes.add(stockHandle, stockValue);
        LogicalTime logicalTime = convertTime( time );
        rtiamb.updateAttributeValues( storageHlaHandle, attributes, "actualize stock".getBytes(), logicalTime );
    }

    private void sendInteraction(double timeStep) throws RTIexception {
        SuppliedParameters parameters =
                RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
        Random random = new Random();
        byte[] id = EncodingHelpers.encodeInt(fedamb.queue.getFirst());
        fedamb.queue.removeFirst();

        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.ZajecieStolika");
        int idHandle = rtiamb.getParameterHandle( "id", interactionHandle );

        parameters.add(idHandle, id);

        LogicalTime time = convertTime( timeStep );
        rtiamb.sendInteraction( interactionHandle, parameters, "tag".getBytes(), time );
        log("Wysłano Zajecie stolika przez: " + id + " czas: " + time );
    }

    private void advanceTime( double timeToAdvance ) throws RTIexception {
        fedamb.isAdvancing = true;
        LogicalTime newTime = convertTime( timeToAdvance );
        rtiamb.timeAdvanceRequest( newTime );

        while( fedamb.isAdvancing )
        {
            rtiamb.tick();
        }
    }


    private void publishAndSubscribe() throws RTIexception {

        int classHandle = rtiamb.getObjectClassHandle("ObjectRoot.Storage");
        int stockHandle    = rtiamb.getAttributeHandle( "stock", classHandle );

        AttributeHandleSet attributes =
                RtiFactoryFactory.getRtiFactory().createAttributeHandleSet();
        attributes.add( stockHandle );
        rtiamb.publishObjectClass(classHandle, attributes);
        //rtiamb.subscribeObjectClassAttributes(classHandle, attributes);

        int classHandle1 = rtiamb.getObjectClassHandle("ObjectRoot.Table");
        int stockHandle1    = rtiamb.getAttributeHandle( "stock", classHandle1 );

        AttributeHandleSet attributes1 =
                RtiFactoryFactory.getRtiFactory().createAttributeHandleSet();
        attributes1.add( stockHandle1 );
        rtiamb.subscribeObjectClassAttributes(classHandle1, attributes1);

        int dojscieDoKolejkiHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.DojscieDoKolejki" );
        fedamb.dojscieDoKolejkiHandle = dojscieDoKolejkiHandle;
        rtiamb.subscribeInteractionClass( dojscieDoKolejkiHandle );

        int getProductHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.ZajecieStolika" );
        rtiamb.publishInteractionClass(getProductHandle);
    }

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
        System.out.println( "KolejkaFederate   : " + message );
    }

    public static void main(String[] args) {
        try {
            new KolejkaFederate().runFederate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
