/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Logic;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

/**
 *
 * @author Kevin-Notebook
 */
public class Solver {
    private IloCplex cplex;
    
    private int T = 2000000000; //large integer (2 billion)
    
    private int R; //total number of residential demand nodes
    private int M; //total number of MRT demand nodes
    private int F; //total number of shopping mall modes (for placement)
    private int I; //total number of POPStation nodes (competition)
    private int S; //Maximum allowable distance (1250)
    private int C; //Maximum holding capacity of a locker for demand
    
    private int p; //Number of new lockers to place
    
    private int[] a;
    private int[] b;
    private double alpha;
    private double beta;
    private int[][] d;
    private int[][] e;
    private int[][] h;
    private int[][] l;
    
    public Solver(int R, int M, int F, int I, int S, int C, int p, double alpha, double beta) throws IloException {
        this.cplex = new IloCplex();
        
        this.R = R;
        this.M = M;
        this.F = F;
        this.I = I;
        this.S = S;
        this.C = C;
        
        this.p = p;
        
        this.alpha = alpha;
        this.beta = beta;
        
        this.a = new int[R];
        this.b = new int[M];
        this.d = new int[R][F];
        this.e = new int[M][F];
        this.h = new int[R][I];
        this.l = new int[M][I];
    }
    
    public void facilityLocation() throws IloException {
        //variables
        IloIntVar[][] c = new IloIntVar[R][F]; //demand from residential r to shopping mall f
        IloIntVar[][] g = new IloIntVar[M][F]; //demand from MRT m to shopping mall f
        IloIntVar[][] j = new IloIntVar[R][I]; //demand from residential r to POPStation i
        IloIntVar[][] k = new IloIntVar[M][I]; //demand from MRT m to POPStation i
        
        for (int i = 0; i < R; i++) {
            c[i] = cplex.intVarArray(F, 0, a[i]);
            j[i] = cplex.intVarArray(I, 0, a[i]);
        }
        
        for (int i = 0; i < M; i++) {
            g[i] = cplex.intVarArray(F, 0, b[i]);
            k[i] = cplex.intVarArray(I, 0, b[i]);
        }
        
        IloIntVar[][] w = new IloIntVar[R][F]; //if demand can flow from residential r to shopping mall f
        IloIntVar[][] x = new IloIntVar[M][F]; //if demand can flow from MRT m to shopping mall f
        IloIntVar[][] n = new IloIntVar[R][I]; //if demand can flow from residential r to POPStation i
        IloIntVar[][] o = new IloIntVar[M][I]; //if demand can flow from MRT m to POPStation i
        
        for (int i = 0; i < R; i++) {
            w[i] = cplex.boolVarArray(F);
            n[i] = cplex.boolVarArray(I);
        }
        
        for (int i = 0; i < M; i++) {
            x[i] = cplex.boolVarArray(F);
            o[i] = cplex.boolVarArray(I);
        }
        
        IloIntVar[] y = cplex.boolVarArray(F);
        IloIntVar[] z = cplex.boolVarArray(I);
        
        //expressions
        IloLinearIntExpr demandObjective = cplex.linearIntExpr();
        for (int i = 0; i < F; i++) {
            for (int i1 = 0; i1 < R; i1++) {
                demandObjective.addTerm(1, c[i1][i]);
            }
            
            for (int i2 = 0; i2 < M; i2++) {
                demandObjective.addTerm(1, g[i2][i]);
            }
        }
        
        IloLinearIntExpr distanceObjective = cplex.linearIntExpr();
        for (int i = 0; i < R; i++) {
            distanceObjective.addTerms(d[i], w[i]);
        }
        for (int i = 0; i < M; i++) {
            distanceObjective.addTerms(e[i], x[i]);
        }
        
        IloLinearIntExpr lockerObjective = cplex.linearIntExpr();
        for (int i = 0; i < F; i++) {
            lockerObjective.addTerm(1, y[i]);
        }
        //define objective
        cplex.addMaximize(demandObjective);
        cplex.addMinimize(distanceObjective);
        cplex.addMinimize(lockerObjective);
        
        //constraints
        //Demand constraints
        for (int i = 0; i < R; i++) {
            cplex.addGe(alpha * a[i], cplex.sum(cplex.sum(c[i]), cplex.sum(j[i])));
        }
        for (int i = 0; i < M; i++) {
            cplex.addGe(beta * b[i], cplex.sum(cplex.sum(g[i]), cplex.sum(k[i])));
        }
        
        //Binary constraints
        for (int i = 0; i < R; i++) {
            for (int i1 = 0; i1 < F; i1++) {
                cplex.addLe(w[i][i1], y[i1]);
            }
        }
        for (int i = 0; i < M; i++) {
            for (int i1 = 0; i1 < F; i1++) {
                cplex.addLe(x[i][i1], y[i1]);
            }
        }
        
        //Flow constraints
        for (int i = 0; i < R; i++) {
            for (int i1 = 0; i1 < F; i++) {
                cplex.addLe(c[i][i1], cplex.prod(T, w[i][i1]));
            }
            for (int i2 = 0; i2 < I; i++) {
                cplex.addLe(j[i][i2], cplex.prod(T, n[i][i2]));
            }
        }
        for (int i = 0; i < M; i++) {
            for (int i1 = 0; i1 < F; i++) {
                cplex.addLe(g[i][i1], cplex.prod(T, x[i][i1]));
            }
            for (int i2 = 0; i2 < I; i++) {
                cplex.addLe(k[i][i2], cplex.prod(T, o[i][i2]));
            }
        }
        
        //Demand constraints
        for (int i = 0; i < R; i++) {
            for (int i1 = 0; i1 < F; i++) {
                cplex.addLe(cplex.prod(d[i][i1], w[i][i1]), S);
            }
            for (int i2 = 0; i2 < I; i++) {
                cplex.addLe(cplex.prod(h[i][i2], n[i][i2]), S);
            }
        }
        for (int i = 0; i < M; i++) {
            for (int i1 = 0; i1 < F; i++) {
                cplex.addLe(cplex.prod(e[i][i1], x[i][i1]), S);
            }
            for (int i2 = 0; i2 < I; i++) {
                cplex.addLe(cplex.prod(l[i][i2], o[i][i2]), S);
            }
        }
        
        //Capacity constraints
        IloLinearIntExpr demandPerLocker = cplex.linearIntExpr();
        for (int i = 0; i < F; i++) {
            for (int i1 = 0; i1 < R; i1++) {
                demandPerLocker.addTerm(1, c[i1][i]);
            }
            for (int i2 = 0; i2 < M; i2++) {
                demandPerLocker.addTerm(1, g[i2][i]);
            }
            
            cplex.addLe(demandPerLocker, C);
        }
        IloLinearIntExpr demandPerPop;
        for (int i = 0; i < I; i++) {
            demandPerPop = cplex.linearIntExpr();
            for (int i1 = 0; i1 < R; i1++) {
                demandPerPop.addTerm(1, j[i1][i]);
            }
            for (int i2 = 0; i2 < M; i2++) {
                demandPerPop.addTerm(1, k[i2][i]);
            }
            
            cplex.addLe(demandPerPop, C);
        }
        
        //Competition constraints
        for (int i = 0; i < R; i++) {
            for (int i1 = 0; i < F; i1++) {
                for (int i2 = 0; i < I; i2++) {
                    cplex.addLe(cplex.prod(d[i][i1], w[i][i1]), cplex.sum(h[i][i2] - 1, cplex.prod(T, cplex.sum(1, cplex.prod(-1, z[i2])))));
                }
            }
        }
        for (int i = 0; i < M; i++) {
            for (int i1 = 0; i < F; i1++) {
                for (int i2 = 0; i < I; i2++) {
                    cplex.addLe(cplex.prod(e[i][i1], x[i][i1]), cplex.sum(l[i][i2] - 1, cplex.prod(T, cplex.sum(1, cplex.prod(-1, z[i2])))));
                }
            }
        }
        for (int i = 0; i < R; i++) {
            for (int i1 = 0; i1 < I; i1++) {
                for (int i2 = 0; i2 < I; i2++) {
                    if (i1 == i2) continue;
                    cplex.addLe(cplex.prod(h[i][i2], n[i][i2]), cplex.sum(h[i][i1], cplex.prod(T, cplex.sum(1, cplex.prod(-1, z[i1])))));
                }
            }
        }
        for (int i = 0; i < M; i++) {
            for (int i1 = 0; i1 < I; i1++) {
                for (int i2 = 0; i2 < I; i2++) {
                    if (i1 == i2) continue;
                    cplex.addLe(cplex.prod(l[i][i2], o[i][i2]), cplex.sum(l[i][i1], cplex.prod(T, cplex.sum(1, cplex.prod(-1, z[i1])))));
                }
            }
        }
        IloLinearNumExpr popTotalDemand;
        
        for (int i = 0; i < I; i++) {
            popTotalDemand = cplex.linearNumExpr();
            for (int i1 = 0; i1 < R; i1++) {
                popTotalDemand.addTerm(1, j[i1][i]);
            }
            for (int i2 = 0; i2 < M; i2++) {
                popTotalDemand.addTerm(1, k[i2][i]);
            }
            cplex.addGe(z[i], cplex.sum(1, cplex.prod(-1, cplex.prod(1.0 / C, popTotalDemand))));
        }
    }
}
