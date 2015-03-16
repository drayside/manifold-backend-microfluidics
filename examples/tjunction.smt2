(set-logic QF_NRA)

(declare-fun rho () Real)
(declare-fun pressure_Cin () Real)
(declare-fun flow_Cin () Real)
(declare-fun res_Cin () Real)
(declare-fun pressure_J () Real)
(declare-fun flow_D () Real)
(declare-fun flow_Ef () Real)
(declare-fun res_Ef () Real)
(declare-fun pressure_G () Real)
(declare-fun flow_Eb () Real)
(declare-fun res_Eb () Real)
(declare-fun pressure_W () Real)

(assert (= rho 0.013503)) ; rho = resistance per micrometer = 8.1016 * 10e-6 * 10000/60
(assert (= res_Cin (* rho 100000)))
(assert (= res_Ef (* rho 50000)))
(assert (= res_Eb (* rho 50000)))
;(assert (= pressure_Cin 1500.0)) ; input pressure = 1500 millibars
(assert (= flow_Cin 7.0)) ; input flow rate = 7 uL/min
(assert (= (- pressure_Cin pressure_J) (* res_Cin flow_Cin))) ; Pin - Pj = Rin * Qin
(assert (= flow_D 7.0)) ; disperse flow rate = 7 uL/min
(assert (= flow_Ef (+ flow_Cin flow_D))) ; Qin + Qd = Qef
(assert (= (- pressure_J pressure_G) (* res_Ef flow_Ef))) ; Pj - Pg = Ref * Qef
(assert (= flow_Ef flow_Eb)) ; Qef = Qeb
(assert (= (- pressure_G pressure_W) (* res_Eb flow_Eb))) ; Pg - Pw = Reb * Qeb
(assert (= pressure_W 1013.0)) ; output pressure = 1013 millibars

(check-sat)
(exit)
