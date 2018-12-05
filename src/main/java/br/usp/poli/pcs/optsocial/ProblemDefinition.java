package br.usp.poli.pcs.optsocial;

import java.util.Arrays;
import org.uma.jmetal.problem.ConstrainedProblem;
import org.uma.jmetal.problem.impl.AbstractDoubleProblem;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.solutionattribute.impl.NumberOfViolatedConstraints;
import org.uma.jmetal.util.solutionattribute.impl.OverallConstraintViolation;

/**
 *
 * @author vinicius
 */
public class ProblemDefinition extends AbstractDoubleProblem implements ConstrainedProblem<DoubleSolution> {

    public OverallConstraintViolation<DoubleSolution> overallConstraintViolationDegree;
    public NumberOfViolatedConstraints<DoubleSolution> numberOfViolatedConstraints;

    /*Problem constants, change them to create a new problem instance*/
    /*You can move some contants to decision variables*/
    protected final double Cw_h;
    protected final double Cw_l;
    protected final double Ck_h;
    protected final double Ck_l;
    protected final double ts;
    protected final int m;
    protected final double Cd_s;
    protected final double Cd_p;

    public ProblemDefinition() {
        this(5, 2, 5, 1, 25, 500, 3, 7);
    }

    public ProblemDefinition(double Cw_h, double Cw_l, double Ck_h, double Ck_l, double ts, int m, double Cd_s, double Cd_p) {
        this.Ck_h = Ck_h;
        this.Ck_l = Ck_l;
        this.Cw_h = Cw_h;
        this.Cw_l = Cw_l;
        this.ts = ts;
        this.m = m;
        this.Cd_s = Cd_s;
        this.Cd_p = Cd_p;
        setNumberOfVariables(7);
        setNumberOfObjectives(3);
        setName("ProblemDefinition");
        setNumberOfConstraints(2);

        // defining the lower and upper limits for each decision variable
        Double[] LOWERLIMIT = {1.0, 1.0, 0.01, 0.1, 0.01, 1.0, 1.0};
        Double[] UPPERLIMIT = {((double)m), ((double)m), 0.5, 5.0, 0.6, 1000.0, 1000.0};
        
        setLowerLimit(Arrays.asList(LOWERLIMIT));
        setUpperLimit(Arrays.asList(UPPERLIMIT));

        overallConstraintViolationDegree = new OverallConstraintViolation<DoubleSolution>();
        numberOfViolatedConstraints = new NumberOfViolatedConstraints<DoubleSolution>();
    }

    @Override
    public void evaluate(DoubleSolution solution) {
        double[] x = new double[solution.getNumberOfVariables()];
        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            x[i] = solution.getVariableValue(i);
        }
        /*Decision variables existing in the solution*/
        int ns = (int) Math.round(x[0]);//number of solo riders
        int np = (int) Math.round(x[1]);//number of pool riders
        double lambda_h = x[2];//Arrival rate of passengers   !where to use it ***** This is used in the "pass and R formula"*****
        double lambda_l = x[3];//Arrival rate of passengers   !where to use it ***** This is used in the "pass and R formula"*****
        double theta = x[4]; //Probability of successful matching of pooling requests
        double v_h = x[5];
        double v_l = x[6];

        double tp = ts + theta / (2 * lambda_l);

        double EC_hp = Cw_h * tp + theta * Ck_h;
        double EC_lp = Cw_l * tp + theta * Ck_l;
        double EC_hs = Cw_h * tp + theta * Ck_h;
        double EC_ls = Cw_l * tp + theta * Ck_l;

        double WTP_hp = v_h - EC_hp; //!where to use it ****** This is only used for those defined constraints 
        double WTP_lp = v_l - EC_lp;
        double WTP_hs = v_h - EC_hs; //!where to use it ****** This is only used for those defined constraints 
        double WTP_ls = v_l - EC_ls; //!where to use it ****** This is only used for those defined constraints 

        double p_s = WTP_lp + EC_hp - EC_hs;
        double p_p = WTP_lp;

        double RSP = p_s * ns + p_p * np;
        double passengerCost = ns * (Cw_h + Ck_h) + np * (Cw_h);//!Cws is not defined
        double driversProfit = (Cd_s * ns + Cd_p * np);//!I miss Driver waiting cost and Cost, Cws is not defined

        solution.setObjective(0, RSP * -1);//maximize
        solution.setObjective(1, passengerCost);//minimize
        solution.setObjective(2, driversProfit);//minimize
    }

    @Override
    public void evaluateConstraints(DoubleSolution solution) {
        double[] constraint = new double[getNumberOfConstraints()];
        double[] x = new double[solution.getNumberOfVariables()];
        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            x[i] = solution.getVariableValue(i);
        }
        int ns = (int) Math.round(x[0]);//number of solo riders
        int np = (int) Math.round(x[1]);//number of pool riders

        if (ns > m) {
            constraint[0] = -1;
        }
        if (np > m) {
            constraint[1] = -1;
        }
        double overallConstraintViolation = 0.0;
        int violatedConstraints = 0;
        for (int i = 0; i < getNumberOfConstraints(); i++) {
            if (constraint[i] < 0.0) {
                overallConstraintViolation += constraint[i];
                violatedConstraints++;
            }
        }

        solution.setAttribute("overallConstraintViolationDegree", overallConstraintViolation);
        overallConstraintViolationDegree.setAttribute(solution, overallConstraintViolation);
        numberOfViolatedConstraints.setAttribute(solution, violatedConstraints);
    }
}
