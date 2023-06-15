package hla13.producerConsumer.Kolejka;

public class Klient {

    private int klientId;
    private double impatientTime;

    public int getKlientId() {
        return klientId;
    }

    public void setKlientId(int klientId) {
        this.klientId = klientId;
    }

    public double getImpatientTime() {
        return impatientTime;
    }

    public void setImpatientTime(double impatientTime) {
        this.impatientTime = impatientTime;
    }

    public Klient(int klientId, double impatientTime) {
        this.klientId = klientId;
        this.impatientTime = impatientTime;
    }
}
