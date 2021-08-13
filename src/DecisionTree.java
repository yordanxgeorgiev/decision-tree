import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class DecisionTree {

    private static final int K_MIN_TEACHING_ROWS = 3;
    private static final int K_FOLD = 10;
    private static final String dataPath = new File("data/breast-cancer.data").getAbsolutePath();
    private ArrayList<String[]> starting_rows = new ArrayList<>();
    private static final int col_count = 10;
    private static final String[] classes = {"recurrence-events","no-recurrence-events"};
    private static final String[] attributes = {"Class", "Age", "Menopaus",
                                                "Tumor-size", "Inv-nodes", "Node-cap",
                                                "Deg-malig", "Breast", "Breast-quad", "Irradiat"};

    private ArrayList<Integer> temp_occurrences = new ArrayList<>();
    private ArrayList<String[]> testSet = new ArrayList<>();

    public DecisionTree()
    {
        // read data from file
        readData();

        int testSize = starting_rows.size()/K_FOLD;
        Node startNode = null;
        double accuracy_sum = 0;

        // k fold cross validation
        for(int i = 0; i < K_FOLD; i++)
        {
            /*int cutStart = i*testSize;
            int cutEnd = (i+1)*testSize;
            if(cutEnd + testSize >= starting_rows.size()) cutEnd = starting_rows.size()-1;
            ArrayList<String[]> trainingData = cut_sublist(starting_rows, cutStart, cutEnd);
            ArrayList<String[]> testData = sublist(starting_rows, cutStart, cutEnd);*/

            ArrayList<String[]> trainingSet = getRandomRows(starting_rows, testSize);
            startNode = teach(trainingSet, null);
            double accuracy = testRows(testSet, startNode);
            accuracy_sum += accuracy;
        }
        System.out.println("Average accuracy: " + accuracy_sum/K_FOLD);
        //startNode.print(1);
    }

    private double testRows(ArrayList<String[]> rows, Node node)
    {
        int correct = 0;
        int mistakes = 0;

        for (String[] row : rows) {
            boolean test = testRow(row, node);
            if (test) correct++;
            else mistakes++;
        }

        return (double)correct/(correct+mistakes);
    }

    private boolean testRow(String[] row, Node node)
    {
        if(node.getValue().equals(classes[0]) || node.getValue().equals(classes[1]))
        {
            String result = node.getValue();
            return result.equals(row[0]);
        }

        String attribute = node.getValue();
        int index = indexOf(attribute);
        String choice = row[index];
        Node[] choices = node.getChildren();
        Node next = null;
        for (Node value : choices) {
            if (choice.equals(value.getValue())) {
                next = value;
                break;
            }
        }
        if(next == null) return true;

        next = next.getChildren()[0];

        return testRow(row, next);
    }

    private int indexOf(String s)
    {
        int index = -1;
        for(int i = 0; i < DecisionTree.attributes.length; i++)
        {
            if(DecisionTree.attributes[i].equals(s))
            {
                index = i;
                break;
            }
        }
        return index;
    }

    private ArrayList<String[]> getRandomRows(ArrayList<String[]> rows, int n)
    {
        testSet = new ArrayList<>();
        ArrayList<String[]> trainingSet = new ArrayList<>(rows);
        Random rnd = new Random();
        for(int i = 0; i < n; i++)
        {
            int randomIndex = rnd.nextInt(trainingSet.size());
            testSet.add(trainingSet.get(randomIndex));
            trainingSet.remove(randomIndex);
        }

        return trainingSet;
    }

    private ArrayList<String[]> cut_sublist(ArrayList<String[]> list, int cutStart, int cutEnd)
    {
        ArrayList<String[]> result = new ArrayList<>();

        for(int i = 0; i < cutStart; i++)
        {
            result.add(list.get(i));
        }

        for(int i = cutEnd+1; i < list.size(); i++)
        {
            result.add(list.get(i));
        }

        return result;
    }

    private ArrayList<String[]> sublist(ArrayList<String[]> list, int start, int end)
    {
        ArrayList<String[]> result = new ArrayList<>();

        for(int i = start; i <= end; i++)
        {
            result.add(list.get(i));
        }

        return result;
    }

    private Node teach(ArrayList<String[]> rows, Node prev)
    {
        if(rows.size() <= K_MIN_TEACHING_ROWS)
        {
            String chosenClass = mostCommonResult(rows);
            return new Node(chosenClass, prev, null);
        }
        int index = maxGainIndex(rows);

        if(entropy1(rows) == 0)
        {
            String value = rows.get(0)[0];
            return new Node(value, prev, null);
        }
        Node node = new Node(attributes[index], prev, null);
        ArrayList<String> categorials = getCategorials(index, rows);
        Node[] children = new Node[categorials.size()];
        node.setChildren(children);
        for(int i = 0; i < categorials.size(); i++)
        {
            children[i] = new Node(categorials.get(i), node, new Node[]{teach(filterRows(index, categorials.get(i), rows), node)});
        }

        return  node;
    }

    private String mostCommonResult(ArrayList<String[]> rows)
    {
        int class1 = 0;
        int class2 = 0;

        for(String[] r : rows)
        {
            if(r[0].equals(classes[0])) class1++;
            else class2++;
        }

        if(class1>class2) return classes[0];
        else return classes[1];
    }

    private int maxGainIndex(ArrayList<String[]> rows)
    {

        double max_gain = -1000000;
        int index = -1;
        for(int i = 1; i < col_count; i++)
        {
            double gain = gain(i, rows);
            if(gain > max_gain)
            {
                max_gain = gain;
                index = i;
            }
        }
        return index;
    }

    private void readData()
    {
        starting_rows = new ArrayList<>();
        try
        {
            // read data from file
            BufferedReader br = new BufferedReader(new FileReader(dataPath));

            String line;
            while ((line = br.readLine()) != null) {

                // split by commas
                String[] row = line.split(",");
                if(!Arrays.asList(row).contains("?"))
                {
                    starting_rows.add(row);
                }
            }
        }
        catch (Exception e){e.printStackTrace();}
    }

    private double entropy(int a, int b)
    {
        if(a == 0 || b == 0) return 0;

        int n = a + b;

        double a_prop = (double) a/n;
        double b_prop = (double) b/n;

        return -(a_prop*Math.log(a_prop)/Math.log(2))
                -(b_prop*Math.log(b_prop)/Math.log(2));
    }

    private double entropy1(ArrayList<String[]> rows)
    {
        int positive = 0, negative = 0;
        for(String[] r: rows)
        {
            if(r[0].equals(classes[0])) positive++;
            else negative++;
        }

        return entropy(positive,negative);
    }

    private double gain(int attr_index, ArrayList<String[]> rows)
    {
        ArrayList<String> categorials = getCategorials(attr_index, rows);
        ArrayList<Integer> occurances = new ArrayList<>(temp_occurrences);

        double sum = 0;
        for(int i = 0; i < categorials.size(); i++)
        {
            sum += (double) occurances.get(i)/rows.size()*entropy1(filterRows(attr_index, categorials.get(i), rows));
        }

        return entropy1(rows) - sum;
    }

    private ArrayList<String[]> filterRows(int attr_index, String attr_value, ArrayList<String[]> rows)
    {
        ArrayList<String[]> result = new ArrayList<>();

        for(String[] r:rows)
        {
            if(r[attr_index].equals(attr_value))
            {
                result.add(r);
            }
        }
        return result;
    }

    private ArrayList<String> getCategorials(int attr_index, ArrayList<String[]> rows)
    {
        ArrayList<String> categorials = new ArrayList<>();
        ArrayList<Integer> occurances = new ArrayList<>();

        for(String[] r: rows)
        {
            if(!categorials.contains(r[attr_index]))
            {
                categorials.add(r[attr_index]);
                occurances.add(1);
            }
            else
            {
                int index = categorials.indexOf(r[attr_index]);
                occurances.set(index,occurances.get(index)+1);
            }
        }

        temp_occurrences = occurances;
        return categorials;
    }
}
