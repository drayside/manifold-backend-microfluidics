mu_water = 1e-3;
wChannel = 100e-6;
hChannel = 25e-6;
% resistance per unit length
rho = (8 * mu_water) / ((wChannel * hChannel^3) * (1 - 0.630*tanh(hChannel / wChannel)));

lengthC = 1000e-6;
lengthEf = lengthC * 0.5;
lengthEb = lengthEf;
lengthG = sqrt(2 * ( (lengthC + lengthEf)^2));

res_C1 = rho * lengthC;
res_C0 = rho * lengthC;
res_C3 = rho * lengthC;
res_C2 = rho * lengthC;
res_G1 = rho * lengthG;
res_G0 = rho * lengthG;
res_G3 = rho * lengthG;
res_G2 = rho * lengthG;
res_E3b = rho * lengthEb;
res_E2b = rho * lengthEb;
res_E1b = rho * lengthEb;
res_E0b = rho * lengthEb;
res_E3f = rho * lengthEf;
res_E2f = rho * lengthEf;
res_E1f = rho * lengthEf;
res_E0f = rho * lengthEf;
flow_Cin = 10 * 1.667e-8;
