import com.neueda.leap.models.Position;
import java.math.BigDecimal;
import java.math.RoundingMode;



public class PositionService {
    
    public void applyBuy(Position position, BigDecimal quantity, BigDecimal price) {
        BigDecimal currentQuantity = position.getQuantity();
        BigDecimal currentAverageCost = position.getAverageCost();

        BigDecimal currentCostBasis = currentAverageCost.multiply(currentQuantity);

        BigDecimal purchaseCost = price.multiply(quantity);
        BigDecimal newQuantity = currentQuantity.add(quantity);
        BigDecimal newAverageCost = currentCostBasis.add(purchaseCost).divide(newQuantity, 4, RoundingMode.HALF_UP);


        position.setQuantity(newQuantity);
        position.setAverageCost(newAverageCost);
    }

    public void applySell(Position position, BigDecimal quantity) {
        BigDecimal currentQuantity = position.getQuantity();

        if (quantity.compareTo(currentQuantity) > 0) {
            throw new IllegalArgumentException("Cannot sell more than the current quantity");
        }

        BigDecimal newQuantity = currentQuantity.subtract(quantity);
        position.setQuantity(newQuantity);
    }
}



