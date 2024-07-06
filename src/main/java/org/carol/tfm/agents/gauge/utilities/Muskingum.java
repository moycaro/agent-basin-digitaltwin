package org.carol.tfm.agents.gauge.utilities;

public class Muskingum {

    public static void main(String[] args) {
        // Ejemplo de uso
        double K = 2.0; // Constante de almacenamiento en horas
        double X = 0.3; // Parámetro de peso
        double deltaT = 1.0; // Intervalo de tiempo en horas

        double[] inflow = {10, 20, 30, 25, 15}; // Caudales de entrada (en m^3/s)
        double[] outflow = new double[inflow.length]; // Caudales de salida (en m^3/s)

        // Inicializar el caudal de salida con el primer valor de entrada
        outflow[0] = inflow[0];

        // Calcular los caudales de salida utilizando el método Muskingum
        for (int t = 1; t < inflow.length; t++) {
            outflow[t] = muskingum(K, X, deltaT, inflow[t], inflow[t - 1], outflow[t - 1]);
        }

        // Imprimir los resultados
        System.out.println("Caudales de salida:");
        for (double q : outflow) {
            System.out.println(q);
        }
    }


    /**
     * Implementa el método Muskingum para calcular el caudal de salida.
     *
     * @param K        Constante de almacenamiento (en horas)
     * @param X        Parámetro de peso (0 <= X <= 0.5)
     * @param deltaT   Intervalo de tiempo (en horas)
     * @param inflowT1 Caudal de entrada en el tiempo t+1 (en m^3/s)
     * @param inflowT  Caudal de entrada en el tiempo t (en m^3/s)
     * @param outflowT Caudal de salida en el tiempo t (en m^3/s)
     * @return Caudal de salida en el tiempo t+1 (en m^3/s)
     */
    public static double muskingum(double K, double X, double deltaT, double inflowT1, double inflowT, double outflowT) {
        // Calcular los coeficientes C0, C1 y C2
        double C0 = (deltaT - 2 * K * X) / (2 * K * (1 - X) + deltaT);
        double C1 = (deltaT + 2 * K * X) / (2 * K * (1 - X) + deltaT);
        double C2 = (2 * K * (1 - X) - deltaT) / (2 * K * (1 - X) + deltaT);

        // Calcular el caudal de salida en el tiempo t+1
        return C0 * inflowT1 + C1 * inflowT + C2 * outflowT;
    }
}
