function res = verifyCosineLawCriticalAngleStrategy(chip, n1, n2, n3)
    import utils.squareIt;

    aX = n1.x - n2.x;
    aY = n1.y - n2.y;
    bX = n3.x - n2.x;
    bY = n3.y - n2.y;

    aDotBSquared = squareIt((aX * bX) + (aY * bY));
    aSquaredBSquared = (squareIt(aX) + squareIt(aY)) * (squareIt(bX) + squareIt(bY))

    cosineSquaredTheta = aDotBSquared / aSquaredBSquared;
    cosineSquaredThetaCritical = squareIt(cos(chip.criticalCrossingAngle));

    res = cosineSquaredThetaCritical <= cosineSquaredTheta;
end
