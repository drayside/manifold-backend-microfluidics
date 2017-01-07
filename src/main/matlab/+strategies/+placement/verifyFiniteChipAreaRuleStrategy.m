function res = verifyFiniteChipAreaRuleStrategy(chip, varargin)
    res = 1;
    for idx = 1:numel(varargin)
        node = varargin{idx};
        res = res & validateNodeCoordinates(chip, node);
    end
end

function res = validateNodeCoordinates(chip, node)
    res = node.x > 0.0 & ...
          node.y > 0.0 & ...
          node.x < chip.maximumChipSizeX & ...
          node.y < chip.maximumChipSizeY;
end
