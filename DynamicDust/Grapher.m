filename = 'data_2012-4-16_22-49-42.txt';

data = importdata(filename);

columns = data.data;

cleaning_per_generation = diff(columns(:, 3));

h1 = plot(columns(:,1), columns(:,2));
set(h1, 'linewidth', 2);

xlabel('Generations');
ylabel('Live cells');
%ylim([0, (max(columns(:,2) + 2000))]);
