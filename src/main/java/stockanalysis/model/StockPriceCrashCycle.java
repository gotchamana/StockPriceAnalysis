package stockanalysis.model;

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
public class StockPriceCrashCycle extends RecursiveTreeObject<StockPriceCrashCycle> {
    
    private StockPrice stockPrice;
    private int inCrashCycle;
}
