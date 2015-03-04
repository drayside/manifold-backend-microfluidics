function res = verifyPythagoreanLengthRuleStrategy(n1, n2, channel)
    import utils.squareIt;

    res = squareIt(n1.x - n2.x) + squareIt(n1.y - n2.y) == channel.len;
end
