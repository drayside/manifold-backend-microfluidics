function res = verifyMinimumChannelLengthStrategy(min_len, varargin)
    import strategies.placement.verifyMinimumChannelLengthRule;

    res = 1;
    for idx = 1:numel(varargin)
        channel = varargin{idx};
        res = res & verifyMinimumChannelLength(min_len, channel);
    end
end

function res = verifyMinimumChannelLength(min_len, channel)
    res = channel.len >= min_len;
end
