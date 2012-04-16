filename = 'data_2012-4-16_19-49-0.txt';

data = importdata(filename);

columns = data.data;

plot(columns(:,1), columns(:,2));

xlabel('Generations');
ylabel('Live cells');
ylim([0, 100*100]);