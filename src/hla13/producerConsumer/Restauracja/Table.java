package hla13.producerConsumer.Restauracja;

public class Table {




    private Double mealTime;
    private Double secondMealTime;



    public Double getMealTime() {
        return mealTime;
    }

    public void setMealTime(Double mealTime) {
        this.mealTime = mealTime;
    }

    public Double getSecondMealTime() {
        return secondMealTime;
    }

    public void setSecondMealTime(Double secondMealTime) {
        this.secondMealTime = secondMealTime;
    }

    public Table(Double mealTime, Double secondMealTime) {

        this.mealTime = mealTime;
        this.secondMealTime = secondMealTime;
    }

    public Table() {
        this.mealTime = 0.0;
        this.secondMealTime = 0.0;
    }
}
