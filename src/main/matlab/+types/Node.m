classdef Node
    properties
        % x, y are assumed to be the co-ordinates of
        % the top-left edge of the node
        x
        y
        area
    end

    methods
        function node = Node(x, y)
            node.x = x;
            node.y = y;
            node.area = 0;
        end
    end

end
